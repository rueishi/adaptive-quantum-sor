package com.nitroj.sor.core;

import com.nitroj.sor.api.BackpressureException;
import com.nitroj.sor.api.MarketDataSnapshotSummary;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.api.SorEventListener;
import com.nitroj.sor.api.SorLifecycleEventTypes;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;
import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.Quote;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import com.nitroj.sor.core.intake.InboundFillRings;
import com.nitroj.sor.core.intake.ParentOrderRing;
import com.nitroj.sor.core.state.MarketBookState;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsibility: first Phase 8 implementation of the public {@link SorEngine}
 * contract.
 *
 * <p>Role in system: bridges the new `sor-api` contract to a running embedded
 * engine shape. P8-06 focuses on lifecycle, warmup, IDs, listener fanout,
 * status, and policy handle behavior; later cards replace the simple intake
 * path with SPI simulators and Agrona rings.</p>
 *
 * <p>Relationships: constructed by {@link SorEngineBuilder} via reflection to
 * preserve `sor-api`'s zero-dependency invariant.</p>
 *
 * <p>Lifecycle: created from a complete builder, warmed up once, used for
 * submissions/cancels/status reads, and closed once.</p>
 *
 * <p>Design intent: keep the first framework implementation intentionally
 * small while making API semantics executable and testable.</p>
 */
public final class SorEngineImpl implements SorEngine, SorControlPlane {
    private final SorConfig config;
    private final Clock clock;
    private final com.nitroj.sor.api.spi.RiskProvider riskProvider;
    private final Persistence persistence;
    private final List<SorEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Long, OrderStatus> statuses = new ConcurrentHashMap<>();
    private final AtomicLong nextParentOrderId = new AtomicLong(1);
    private final AtomicLong nextChildOrderId = new AtomicLong(1);
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService fanout;
    private final PolicyHandle activePolicy;
    private final ParentOrderRing orderRing;
    private final ManyToOneRingBuffer orderRingBuffer;
    private final InboundFillRings inboundFillRings = new InboundFillRings();
    private final Observability observability;
    private final MarketBookState marketBookState;
    private final EngineOrderBook orderBook = new EngineOrderBook();
    private final RingWriter[] childOrderWriters;
    private volatile SorResetSummary lastResetSummary;

    private SorEngineImpl(final SorEngineBuilder builder) {
        this.config = builder.config();
        this.clock = builder.clock();
        this.riskProvider = builder.riskProvider();
        this.persistence = builder.persistence();
        this.observability = builder.observability();
        this.marketBookState = new MarketBookState(config.instrumentCount(), config.venueCount());
        this.childOrderWriters = new RingWriter[config.venueCount()];
        for (int venueId = 0; venueId < childOrderWriters.length; venueId++) {
            childOrderWriters[venueId] = builder.venueAdapter().childOrderRingWriter(venueId);
        }
        this.orderRing = new ParentOrderRing(config.orderQueueCapacity());
        this.orderRingBuffer = orderRing.ringBuffer();
        this.fanout = Executors.newSingleThreadExecutor(new NamedThreadFactory("sor-event-fanout"));
        this.activePolicy = new BasicPolicyHandle(1, 1, sha256("phase8-policy-v1"), clock.epochNanos(), clock.epochNanos());
        final MarketDataListener marketDataListener = new EngineMarketDataListener();
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                builder.marketData().subscribe(instrumentId, venueId, marketDataListener);
            }
        }
        builder.venueAdapter().callback(new com.nitroj.sor.api.spi.VenueAdapter.VenueAdapterCallback() {
            @Override public void deliverFill(final com.nitroj.sor.api.spi.FillReport report) {
                inboundFillRings.ringForVenue(report.venueId());
                final EngineOrderBook.ParentSnapshot snapshot = orderBook.applyFill(report);
                if (snapshot != null) {
                    statuses.put(snapshot.parentOrderId(), orderStatus(snapshot));
                }
                appendLifecycle(SorLifecycleEventTypes.FILL_DELIVERED, report.parentOrderId(), report.filledEpochNanos());
                emit(new SorEvent.Filled(report.childOrderId(), report.parentOrderId(), report.venueId(), report.filledQuantity(), report.fillPrice(), report.filledEpochNanos()));
            }
            @Override public void deliverReject(final com.nitroj.sor.api.spi.RejectReport report) {
                final EngineOrderBook.ParentSnapshot snapshot = orderBook.applyReject(report);
                if (snapshot != null) {
                    statuses.put(snapshot.parentOrderId(), orderStatus(snapshot));
                }
                appendLifecycle(SorLifecycleEventTypes.REJECT_DELIVERED, report.parentOrderId(), report.rejectedEpochNanos());
                emit(new SorEvent.Rejected(report.childOrderId(), report.parentOrderId(), report.venueId(), report.reasonCode(), report.rejectedEpochNanos()));
            }
        });
    }

    /**
     * Creates an engine from the public builder.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    public static SorEngine create(final SorEngineBuilder builder) {
        return new SorEngineImpl(builder);
    }

    @Override
    public void warmup(final int syntheticOrderCount) {
        if (syntheticOrderCount < 0) {
            throw new IllegalArgumentException("syntheticOrderCount must be non-negative");
        }
        ensureOpen();
        final long deadline = clock.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(1, config.closeDrainTimeoutMillis()));
        final RiskCheckRequest request = new RiskCheckRequest();
        final RiskDecision decision = new RiskDecision();
        for (int i = 0; i < syntheticOrderCount; i++) {
            riskProvider.check(request.set(i + 1L, 0, 1, 1), decision);
            if (clock.nanoTime() > deadline) {
                ready.set(false);
                throw new WarmupTimeoutException("warmup timed out after " + i + " synthetic orders");
            }
        }
        ready.set(true);
        observability.recordPolicyPublished(activePolicy.version(), activePolicy.hash64(), 0);
        appendLifecycle(SorLifecycleEventTypes.POLICY_PUBLISHED, activePolicy.version(), clock.epochNanos());
        emit(new SorEvent.PolicyPublished(activePolicy.version(), activePolicy.hash64(), clock.epochNanos()));
    }

    @Override public boolean isReady() { return ready.get(); }

    @Override
    public long submitParentOrder(final ParentOrderRequest request) {
        ensureOpen();
        if (!ready.get()) {
            throw new IllegalStateException("engine must be warmed up before submitParentOrder");
        }
        final long id = nextParentOrderId.getAndIncrement();
        final long started = clock.nanoTime();
        final long now = clock.epochNanos();
        if (!orderRing.offer(id, request, now)) {
            observability.recordBackpressureRejected(1);
            emit(new SorEvent.BackpressureRejected(id, 1, now));
            throw new BackpressureException("parent order ring is full for orderQueueCapacity=" + config.orderQueueCapacity());
        }
        final EngineOrderBook.ParentSnapshot snapshot = orderBook.acceptParent(id, request.quantity(), now);
        statuses.put(id, orderStatus(snapshot));
        observability.recordParentOrderRingDepth(orderRing.depth());
        observability.recordRouteDecisionLatency(Math.max(0, clock.nanoTime() - started));
        emitChildOrder(id, request, now);
        appendLifecycle(SorLifecycleEventTypes.ROUTE_DECIDED, id, now);
        emit(new SorEvent.RouteDecided(id, activePolicy.version(), activePolicy.hash64(), 0, 0, request.quantity(), now));
        return id;
    }

    /**
     * Drains parent-order intake messages for tests and later engine workers.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    public int drainOrderIntake(final int limit) {
        return orderRing.drain(limit);
    }

    /**
     * Returns current parent-order intake depth for observability adapters.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    public int orderRingDepth() {
        return orderRing.depth();
    }

    /**
     * Returns the engine-owned market book for package-local tests and future
     * control-plane summaries.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    MarketBookState marketBookState() {
        return marketBookState;
    }

    /**
     * Returns the engine-owned working order book for package-local tests and
     * future control-plane summaries.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    EngineOrderBook orderBook() {
        return orderBook;
    }

    @Override
    public void cancelParentOrder(final long parentOrderId) {
        if (closed.get() || parentOrderId <= 0) {
            return;
        }
        statuses.computeIfPresent(parentOrderId, (id, old) -> old.status() == OrderStatusCode.CANCELLED
                ? old
                : new OrderStatus(id, OrderStatusCode.CANCELLED, old.originalQuantity(), old.filledQuantity(), old.remainingQuantity(), clock.epochNanos()));
    }

    @Override public Optional<OrderStatus> getOrderStatus(final long parentOrderId) { return Optional.ofNullable(statuses.get(parentOrderId)); }

    @Override
    public Registration registerListener(final SorEventListener listener) {
        listeners.add(java.util.Objects.requireNonNull(listener, "listener must not be null"));
        return Registration.of(() -> listeners.remove(listener));
    }

    @Override public PolicyHandle activePolicy() { return activePolicy; }

    @Override
    public SorResetSummary reset(final SorResetRequest request) {
        java.util.Objects.requireNonNull(request, "request must not be null");
        ensureOpen();
        if (request.mode().destructive() && !config.destructiveResetEnabled()) {
            return auditReset(summary(request, false, "destructive reset disabled by configuration",
                    new String[]{}, new String[]{"marketBook", "orderBook", "activePolicy"}, new String[]{}));
        }
        if (request.requireNoLiveOrders() && orderBook.hasLiveOrders()) {
            return auditReset(summary(request, false, "live orders present; reset rejected",
                    new String[]{}, new String[]{"marketBook", "orderBook", "activePolicy"}, new String[]{}));
        }
        final String[] cleared = switch (request.mode()) {
            case CLEAR_MARKET_DATA -> {
                marketBookState.clear();
                yield new String[]{"marketBook"};
            }
            case PURGE_RUNTIME_STATE, KEEP_POLICY_PURGE_RUNTIME, PURGE_AND_REPOPULATE,
                    SCENARIO_REPLAY_RESET, RECOVERY_REBUILD -> {
                marketBookState.clear();
                orderBook.clear();
                statuses.clear();
                yield new String[]{"marketBook", "orderBook", "orderStatus"};
            }
            case APPEND -> new String[]{};
        };
        final String[] kept = request.mode() == SorResetMode.PURGE_RUNTIME_STATE
                ? new String[]{"activePolicy"}
                : new String[]{"activePolicy"};
        final String[] repopulated = request.mode() == SorResetMode.PURGE_AND_REPOPULATE
                || request.mode() == SorResetMode.SCENARIO_REPLAY_RESET
                ? new String[]{"awaitingScenarioReplay"} : new String[]{};
        return auditReset(summary(request, true, "reset accepted", cleared, kept, repopulated));
    }

    @Override
    public SorStateSummary stateSummary() {
        return new SorStateSummary(
                orderBook.parentCount(),
                orderBook.childCount(),
                orderBook.pendingChildQuantity(),
                activePolicy.version(),
                lastResetSummary);
    }

    @Override
    public MarketDataSnapshotSummary marketDataSnapshot() {
        return new MarketDataSnapshotSummary(
                marketBookState.sequence(),
                marketBookState.checksum(),
                marketBookState.populatedCellCount(),
                marketBookState.lastUpdateEpochNanos());
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            fanout.shutdown();
            try {
                fanout.awaitTermination(config.closeDrainTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("engine is closed");
        }
    }

    private void emit(final SorEvent event) {
        fanout.execute(() -> listeners.forEach(listener -> listener.onEvent(event)));
    }

    private void emitChildOrder(final long parentOrderId, final ParentOrderRequest request, final long now) {
        final int venueId = 0;
        final long childOrderId = nextChildOrderId.getAndIncrement();
        final ChildOrderRef child = new ChildOrderRef().set(childOrderId, parentOrderId, venueId, request.side(),
                request.quantity(), 0, now);
        if (!childOrderWriters[venueId].offer(child)) {
            observability.recordBackpressureRejected(2);
            emit(new SorEvent.BackpressureRejected(parentOrderId, 2, now));
            throw new BackpressureException("child order ring is full for venueId=" + venueId);
        }
        orderBook.recordChildOrder(childOrderId, parentOrderId, venueId, request.quantity(), now);
        appendLifecycle(SorLifecycleEventTypes.CHILD_ORDER_EMITTED, childOrderId, now);
        emit(new SorEvent.ChildOrderEmitted(childOrderId, parentOrderId, venueId, request.side(), request.quantity(), now));
    }

    private SorResetSummary auditReset(final SorResetSummary summary) {
        lastResetSummary = summary;
        final long eventType = summary.accepted()
                ? SorLifecycleEventTypes.RESET_ACCEPTED
                : SorLifecycleEventTypes.RESET_REJECTED;
        appendLifecycle(eventType, summary.mode().ordinal(), clock.epochNanos());
        return summary;
    }

    private void appendLifecycle(final long eventType, final long subjectId, final long epochNanos) {
        persistence.appendLifecycleEvent(new LifecycleEvent().set(eventType, subjectId, epochNanos));
    }

    private static SorResetSummary summary(final SorResetRequest request, final boolean accepted,
                                           final String message, final String[] cleared,
                                           final String[] kept, final String[] repopulated) {
        return new SorResetSummary(request.mode(), accepted, request.mode() != SorResetMode.APPEND,
                message, cleared, kept, repopulated);
    }

    private static OrderStatus orderStatus(final EngineOrderBook.ParentSnapshot snapshot) {
        return new OrderStatus(snapshot.parentOrderId(), snapshot.status(), snapshot.originalQuantity(),
                snapshot.filledQuantity(), snapshot.remainingQuantity(), snapshot.updatedEpochNanos());
    }

    /**
     * Copies reusable market-data carrier fields into the engine-owned market
     * book.
     *
     * <p>Hot-path callback. The listener never stores the mutable
     * {@link Quote}; invalid market data is rejected by
     * {@link MarketBookState#updateTopOfBook(int, int, long, long, long, long)}
     * before any state mutation occurs.</p>
     */
    private final class EngineMarketDataListener implements MarketDataListener {
        @Override
        public void onQuote(final Quote quote) {
            try {
                marketBookState.updateTopOfBook(
                        quote.instrumentId(),
                        quote.venueId(),
                        quote.bidPrice(),
                        quote.askPrice(),
                        quote.bidQuantity(),
                        quote.askQuantity(),
                        quote.epochNanos());
            } catch (IllegalArgumentException ex) {
                appendLifecycle(SorLifecycleEventTypes.MARKET_DATA_REJECTED,
                        subjectId(quote.instrumentId(), quote.venueId()), quote.epochNanos());
                throw ex;
            }
        }
    }

    private static long subjectId(final int instrumentId, final int venueId) {
        return ((long) instrumentId << 32) ^ (venueId & 0xffff_ffffL);
    }

    private static byte[] sha256(final String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record BasicPolicyHandle(long version, long hash64, byte[] hashSha256,
                                     long createdEpochNanos, long effectiveFromEpochNanos) implements PolicyHandle {
        private BasicPolicyHandle {
            hashSha256 = Arrays.copyOf(hashSha256, hashSha256.length);
        }
        @Override public byte[] hashSha256() { return Arrays.copyOf(hashSha256, hashSha256.length); }
    }

    private record NamedThreadFactory(String prefix) implements ThreadFactory {
        @Override public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable);
            thread.setName(prefix + "-1");
            thread.setDaemon(true);
            return thread;
        }
    }
}
