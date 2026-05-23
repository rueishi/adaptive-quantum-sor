package com.nitroj.sor.core;

import com.nitroj.sor.api.BackpressureException;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.api.SorEventListener;
import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.Quote;
import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import com.nitroj.sor.core.intake.InboundFillRings;
import com.nitroj.sor.core.intake.ParentOrderRing;
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
public final class SorEngineImpl implements SorEngine {
    private final SorConfig config;
    private final Clock clock;
    private final com.nitroj.sor.api.spi.RiskProvider riskProvider;
    private final List<SorEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Long, OrderStatus> statuses = new ConcurrentHashMap<>();
    private final AtomicLong nextParentOrderId = new AtomicLong(1);
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService fanout;
    private final PolicyHandle activePolicy;
    private final ParentOrderRing orderRing;
    private final ManyToOneRingBuffer orderRingBuffer;
    private final InboundFillRings inboundFillRings = new InboundFillRings();
    private final Observability observability;

    private SorEngineImpl(final SorEngineBuilder builder) {
        this.config = builder.config();
        this.clock = builder.clock();
        this.riskProvider = builder.riskProvider();
        this.observability = builder.observability();
        this.orderRing = new ParentOrderRing(config.orderQueueCapacity());
        this.orderRingBuffer = orderRing.ringBuffer();
        this.fanout = Executors.newSingleThreadExecutor(new NamedThreadFactory("sor-event-fanout"));
        this.activePolicy = new BasicPolicyHandle(1, 1, sha256("phase8-policy-v1"), clock.epochNanos(), clock.epochNanos());
        builder.marketData().subscribe(0, new MarketDataListener() {
            @Override
            public void onQuote(final Quote quote) {
                // P8-06 only proves the callback seam; P8-07 populates engine state from it.
            }
        });
        builder.venueAdapter().callback(new com.nitroj.sor.api.spi.VenueAdapter.VenueAdapterCallback() {
            @Override public void deliverFill(final com.nitroj.sor.api.spi.FillReport report) {
                inboundFillRings.ringForVenue(report.venueId());
                emit(new SorEvent.Filled(report.childOrderId(), report.parentOrderId(), report.venueId(), report.filledQuantity(), report.fillPrice(), report.filledEpochNanos()));
            }
            @Override public void deliverReject(final com.nitroj.sor.api.spi.RejectReport report) { emit(new SorEvent.Rejected(report.childOrderId(), report.parentOrderId(), report.venueId(), report.reasonCode(), report.rejectedEpochNanos())); }
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
        final OrderStatus status = new OrderStatus(id, OrderStatusCode.ACCEPTED, request.quantity(), 0, request.quantity(), now);
        statuses.put(id, status);
        observability.recordParentOrderRingDepth(orderRing.depth());
        observability.recordRouteDecisionLatency(Math.max(0, clock.nanoTime() - started));
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
