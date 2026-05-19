package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;
import com.nitroj.adaptive.quantum.sor.metadata.InstrumentMetadata;
import com.nitroj.adaptive.quantum.sor.metadata.VenueMetadata;
import com.nitroj.adaptive.quantum.sor.model.ChildOrder;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioControlService;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioEngineContext;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioChildFill;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioParentOrderIntent;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioParentOrderResult;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioResetRequest;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioRunRequest;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioRunResult;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsibility: expose the Phase 1 control-plane HTTP API.
 *
 * <p>Role in system: Jupyter notebooks use this server to submit parent orders,
 * inspect order status, read stats/policy identity, and stream lifecycle events.</p>
 *
 * <p>Relationships: writes {@link ParentOrderIntentQueue}, reads
 * {@link PolicyPublisher} and {@link InMemoryLifecycleEventStore}.</p>
 *
 * <p>Lifecycle: created and started by tests or future application startup, then
 * stopped when the process shuts down.</p>
 *
 * <p>Design intent: use the JDK HTTP server to keep the Adaptive Quantum SOR dependency-free and
 * clearly control-plane only.</p>
 */
public final class SorHttpApiServer {
    private final ParentOrderIntentQueue orderQueue;
    private final PolicyPublisher publisher;
    private final InMemoryLifecycleEventStore lifecycleStore;
    private final ScenarioControlService scenarioControlService;
    private final PolicyDrivenSorExecutioner executioner;
    private final ChildOrderBuffer childOrderBuffer;
    private final MarketBookState marketBookState;
    private final InstrumentMetadata instrumentMetadata;
    private final VenueMetadata venueMetadata;
    private final CumulativeOrderStats cumulativeOrderStats = new CumulativeOrderStats();
    private final Map<Long, OrderStatusView> orders = new HashMap<>();
    private final HttpServer server;
    private long nextParentOrderId = 1L;

    public SorHttpApiServer(
            final int port,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore
    ) throws IOException {
        this(port, orderQueue, publisher, lifecycleStore, SorConfig.defaults());
    }

    public SorHttpApiServer(
            final int port,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore,
            final SorConfig scenarioConfig
    ) throws IOException {
        this(port, orderQueue, publisher, lifecycleStore, scenarioConfig, null, null, null, null, null);
    }

    public SorHttpApiServer(
            final int port,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore,
            final SorConfig scenarioConfig,
            final PolicyDrivenSorExecutioner executioner,
            final ChildOrderBuffer childOrderBuffer
    ) throws IOException {
        this(port, orderQueue, publisher, lifecycleStore, scenarioConfig, executioner, childOrderBuffer, null, null, null);
    }

    public SorHttpApiServer(
            final int port,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore,
            final SorConfig scenarioConfig,
            final PolicyDrivenSorExecutioner executioner,
            final ChildOrderBuffer childOrderBuffer,
            final MarketBookState marketBookState
    ) throws IOException {
        this(port, orderQueue, publisher, lifecycleStore, scenarioConfig, executioner, childOrderBuffer,
                marketBookState, null, null);
    }

    public SorHttpApiServer(
            final int port,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore,
            final SorConfig scenarioConfig,
            final PolicyDrivenSorExecutioner executioner,
            final ChildOrderBuffer childOrderBuffer,
            final MarketBookState marketBookState,
            final InstrumentMetadata instrumentMetadata,
            final VenueMetadata venueMetadata
    ) throws IOException {
        if (orderQueue == null || publisher == null || lifecycleStore == null) {
            throw new IllegalArgumentException("api dependencies must not be null");
        }
        if (scenarioConfig == null) {
            throw new IllegalArgumentException("scenarioConfig must not be null");
        }
        if ((executioner == null) != (childOrderBuffer == null)) {
            throw new IllegalArgumentException("executioner and childOrderBuffer must be supplied together");
        }
        this.orderQueue = orderQueue;
        this.publisher = publisher;
        this.lifecycleStore = lifecycleStore;
        this.executioner = executioner;
        this.childOrderBuffer = childOrderBuffer;
        this.marketBookState = marketBookState;
        this.instrumentMetadata = instrumentMetadata;
        this.venueMetadata = venueMetadata;
        this.scenarioControlService = new ScenarioControlService(new ScenarioEngineContext(
                scenarioConfig, orderQueue, publisher, lifecycleStore));
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/orders", this::handleOrders);
        server.createContext("/orders/summary", this::handleOrderSummary);
        server.createContext("/stats/current", this::handleStats);
        server.createContext("/policy/current", this::handlePolicy);
        server.createContext("/events/stream", this::handleEvents);
        server.createContext("/scenario/reset", this::handleScenarioReset);
        server.createContext("/scenario/run", this::handleScenarioRun);
        server.createContext("/scenario/summary", this::handleScenarioSummary);
        server.createContext("/scenario/events", this::handleEvents);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleOrders(final HttpExchange exchange) throws IOException {
        try {
            if ("POST".equals(exchange.getRequestMethod()) && "/orders".equals(exchange.getRequestURI().getPath())) {
                final OrderRequest request = OrderRequest.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                final long id = nextParentOrderId++;
                final OrderIntent intent = request.toIntent(id, Math.max(1L, System.nanoTime()));
                if (!orderQueue.offer(intent)) {
                    send(exchange, 503, "order queue is full", "text/plain");
                    return;
                }
                final OrderStatusView view = new OrderStatusView();
                view.parentOrderId = id;
                view.remainingQty = request.quantity();
                view.status = OrderStatus.NEW;
                if (executioner != null) {
                    final OrderIntent executableIntent = orderQueue.poll();
                    final RouteDecisionResult route = executioner.route(executableIntent, childOrderBuffer);
                    view.filledQty = route.routedQty;
                    view.remainingQty = route.residualQty;
                    view.status = route.status;
                    view.childOrders = copyChildOrders(childOrderBuffer);
                    cumulativeOrderStats.record(id, view.filledQty, view.remainingQty, route.childOrderCount,
                            fillsFromChildren(view.childOrders, 1));
                    lifecycleStore.append(new LifecycleEvent(id, Math.max(1L, System.nanoTime()), 1,
                            LifecycleEventType.SOR_DECISION, id,
                            "order routed " + id + " childOrders=" + route.childOrderCount + " residual=" + route.residualQty));
                } else {
                    cumulativeOrderStats.record(id, 0L, view.remainingQty, 0, new ScenarioChildFill[0]);
                }
                orders.put(id, view);
                lifecycleStore.append(new LifecycleEvent(id, Math.max(1L, System.nanoTime()), 1, LifecycleEventType.SIM_PARENT_ORDER, id, "order accepted " + id));
                send(exchange, 200, view.toJson(), "application/json");
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                final String path = exchange.getRequestURI().getPath();
                final long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                final OrderStatusView view = orders.get(id);
                send(exchange, view == null ? 404 : 200, view == null ? "{}" : view.toJson(), "application/json");
                return;
            }
            send(exchange, 405, "method not allowed", "text/plain");
        } catch (RuntimeException ex) {
            send(exchange, 400, ex.getMessage(), "text/plain");
        }
    }

    private void handleOrderSummary(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        send(exchange, 200, cumulativeOrderStats.toJson(), "application/json");
    }

    private void handleStats(final HttpExchange exchange) throws IOException {
        final StatsSnapshotView view = new StatsSnapshotView();
        view.policyVersion = publisher.activePolicy() == null ? 0L : publisher.activePolicy().policyVersion;
        view.venueCount = publisher.activePolicy() == null ? 0 : publisher.activePolicy().fullPolicyMatrix.venueCount;
        send(exchange, 200, view.toJson(), "application/json");
    }

    private void handlePolicy(final HttpExchange exchange) throws IOException {
        send(exchange, 200, new PolicySnapshotView(publisher.activePolicy()).toJson(), "application/json");
    }

    private void handleEvents(final HttpExchange exchange) throws IOException {
        send(exchange, 200, new EventStreamHandler(lifecycleStore).render(), "text/event-stream");
    }

    private void handleScenarioReset(final HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "method not allowed", "text/plain");
                return;
            }
            final ScenarioResetRequest request = ScenarioResetRequest.parse(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            send(exchange, 200, scenarioControlService.reset(request).toJson(), "application/json");
        } catch (RuntimeException ex) {
            send(exchange, 400, ex.getMessage(), "text/plain");
        }
    }

    private void handleScenarioRun(final HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "method not allowed", "text/plain");
                return;
            }
            final ScenarioRunRequest request = ScenarioRunRequest.parse(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final ScenarioRunResult run = scenarioControlService.run(request);
            final ScenarioParentOrderResult[] parentOrderResults = routeParentOrders(request);
            send(exchange, 200, new ScenarioRunResult(
                    run.success(),
                    run.replaySafe(),
                    run.message(),
                    run.resetSummary(),
                    run.summary(),
                    parentOrderResults
            ).toJson(), "application/json");
        } catch (RuntimeException ex) {
            send(exchange, 400, ex.getMessage(), "text/plain");
        }
    }

    private void handleScenarioSummary(final HttpExchange exchange) throws IOException {
        final var summary = scenarioControlService.lastSummary();
        if (summary == null) {
            send(exchange, 200, "{}", "application/json");
            return;
        }
        send(exchange, 200, "{\"scenarioId\":\"" + summary.scenarioId()
                + "\",\"seed\":" + summary.seed()
                + ",\"ticksRun\":" + summary.ticksRun()
                + ",\"bookChecksum\":" + summary.bookChecksum() + "}", "application/json");
    }

    private static void send(final HttpExchange exchange, final int status, final String body, final String contentType) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static ChildOrder[] copyChildOrders(final ChildOrderBuffer buffer) {
        final ChildOrder[] copy = new ChildOrder[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) {
            final ChildOrder source = buffer.get(i);
            final ChildOrder target = new ChildOrder();
            target.set(
                    source.childOrderId,
                    source.parentOrderId,
                    source.instrumentId,
                    source.venueId,
                    source.side,
                    source.quantity,
                    source.policyVersion,
                    source.policyHash64
            );
            target.status = source.status;
            copy[i] = target;
        }
        return copy;
    }

    private ScenarioParentOrderResult[] routeParentOrders(final ScenarioRunRequest request) {
        final ScenarioParentOrderIntent[] parentOrders = request.parentOrders();
        final ScenarioParentOrderResult[] results = new ScenarioParentOrderResult[parentOrders.length];
        for (int i = 0; i < parentOrders.length; i++) {
            final long id = nextParentOrderId++;
            final long deadline = System.nanoTime() + request.routeTimeoutMillis() * 1_000_000L;
            long remaining = parentOrders[i].quantity();
            long filled = 0L;
            int status = OrderStatus.NEW;
            int routeAttempts = 0;
            boolean timedOut = false;
            final java.util.List<ScenarioChildFill> fills = new java.util.ArrayList<>();
            final OrderStatusView view = new OrderStatusView();
            view.parentOrderId = id;
            view.remainingQty = remaining;
            view.status = status;
            final java.util.List<ChildOrder> copiedChildren = new java.util.ArrayList<>();
            while (remaining > 0 && routeAttempts < request.maxRouteAttempts()) {
                if (System.nanoTime() > deadline) {
                    timedOut = true;
                    break;
                }
                final OrderIntent intent = new OrderIntent(id, parentOrders[i].instrumentId(), parentOrders[i].side(),
                        remaining, parentOrders[i].urgencyId(), Math.max(1L, System.nanoTime()));
                if (!orderQueue.offer(intent)) {
                    throw new IllegalStateException("order queue is full");
                }
                if (executioner == null) {
                    status = OrderStatus.NEW;
                    break;
                }
                routeAttempts++;
                final RouteDecisionResult route = executioner.route(orderQueue.poll(), childOrderBuffer);
                status = route.status;
                if (route.childOrderCount == 0 || route.routedQty <= 0) {
                    remaining = route.residualQty;
                    break;
                }
                for (int childIndex = 0; childIndex < childOrderBuffer.size(); childIndex++) {
                    final ChildOrder child = childOrderBuffer.get(childIndex);
                    final long priceTicks = fillPriceTicks(child);
                    final long fillQty = child.quantity;
                    fills.add(new ScenarioChildFill(
                            child.childOrderId,
                            child.parentOrderId,
                            routeAttempts,
                            child.instrumentId,
                            instrumentLabel(child.instrumentId),
                            child.venueId,
                            venueLabel(child.venueId),
                            child.side,
                            sideLabel(child.side),
                            child.quantity,
                            fillQty,
                            priceTicks,
                            fillQty * priceTicks,
                            child.policyVersion,
                            child.policyHash64
                    ));
                    copiedChildren.add(copyChildOrder(child));
                }
                filled += route.routedQty;
                remaining = route.residualQty;
                lifecycleStore.append(new LifecycleEvent(id, Math.max(1L, System.nanoTime()), 1,
                        LifecycleEventType.SOR_DECISION, id,
                        "scenario parent order routed " + id + " attempt=" + routeAttempts
                                + " childOrders=" + route.childOrderCount + " residual=" + route.residualQty));
            }
            if (remaining == 0L) {
                status = OrderStatus.FILLED;
            } else if (filled > 0L) {
                status = OrderStatus.PARTIALLY_FILLED;
            }
            view.filledQty = filled;
            view.remainingQty = remaining;
            view.status = status;
            view.childOrders = copiedChildren.toArray(ChildOrder[]::new);
            orders.put(id, view);
            results[i] = new ScenarioParentOrderResult(
                    view.parentOrderId,
                    view.filledQty,
                    view.remainingQty,
                    view.status,
                    view.childOrders == null ? 0 : view.childOrders.length,
                    routeAttempts,
                    timedOut,
                    fills.toArray(ScenarioChildFill[]::new)
            );
            cumulativeOrderStats.record(results[i]);
        }
        return results;
    }

    private ScenarioChildFill[] fillsFromChildren(final ChildOrder[] children, final int routeAttempt) {
        final ScenarioChildFill[] fills = new ScenarioChildFill[children == null ? 0 : children.length];
        for (int i = 0; i < fills.length; i++) {
            final ChildOrder child = children[i];
            final long priceTicks = fillPriceTicks(child);
            final long fillQty = child.quantity;
            fills[i] = new ScenarioChildFill(
                    child.childOrderId,
                    child.parentOrderId,
                    routeAttempt,
                    child.instrumentId,
                    instrumentLabel(child.instrumentId),
                    child.venueId,
                    venueLabel(child.venueId),
                    child.side,
                    sideLabel(child.side),
                    child.quantity,
                    fillQty,
                    priceTicks,
                    fillQty * priceTicks,
                    child.policyVersion,
                    child.policyHash64
            );
        }
        return fills;
    }

    private long fillPriceTicks(final ChildOrder child) {
        if (marketBookState == null) {
            return 0L;
        }
        return child.side == com.nitroj.adaptive.quantum.sor.model.Side.BUY
                ? marketBookState.askPriceTicks(child.instrumentId, child.venueId)
                : marketBookState.bidPriceTicks(child.instrumentId, child.venueId);
    }

    private String instrumentLabel(final int instrumentId) {
        return instrumentMetadata == null ? "INST" + instrumentId : instrumentMetadata.symbol(instrumentId);
    }

    private String venueLabel(final int venueId) {
        return venueMetadata == null ? "VENUE" + venueId : venueMetadata.venueName(venueId);
    }

    private static String sideLabel(final int side) {
        if (side == Side.BUY) {
            return "BUY";
        }
        if (side == Side.SELL) {
            return "SELL";
        }
        return "UNKNOWN";
    }

    private static ChildOrder copyChildOrder(final ChildOrder source) {
        final ChildOrder target = new ChildOrder();
        target.set(
                source.childOrderId,
                source.parentOrderId,
                source.instrumentId,
                source.venueId,
                source.side,
                source.quantity,
                source.policyVersion,
                source.policyHash64
        );
        target.status = source.status;
        return target;
    }

    private static final class CumulativeOrderStats {
        private final Map<String, Bucket> byInstrument = new HashMap<>();
        private final Map<String, Bucket> byVenue = new HashMap<>();
        private final Map<String, Bucket> byInstrumentVenue = new HashMap<>();
        private long parentOrderCount;
        private long childOrderCount;
        private long filledQty;
        private long remainingQty;
        private long notionalTicks;

        synchronized void record(final ScenarioParentOrderResult result) {
            record(result.parentOrderId(), result.filledQty(), result.remainingQty(), result.childOrderCount(), result.fills());
        }

        synchronized void record(
                final long parentOrderId,
                final long parentFilledQty,
                final long parentRemainingQty,
                final long parentChildOrderCount,
                final ScenarioChildFill[] fills
        ) {
            parentOrderCount++;
            childOrderCount += parentChildOrderCount;
            filledQty += parentFilledQty;
            remainingQty += parentRemainingQty;
            final List<String> parentInstruments = new ArrayList<>();
            for (ScenarioChildFill fill : fills) {
                notionalTicks += fill.notionalTicks();
                recordFill(byInstrument.computeIfAbsent("instrument:" + fill.instrumentId(), key -> Bucket.instrument(fill)),
                        parentOrderId, parentInstruments, fill);
                recordFill(byVenue.computeIfAbsent("venue:" + fill.venueId(), key -> Bucket.venue(fill)),
                        parentOrderId, null, fill);
                recordFill(byInstrumentVenue.computeIfAbsent("instrumentVenue:" + fill.instrumentId() + ":" + fill.venueId(),
                                key -> Bucket.instrumentVenue(fill)),
                        parentOrderId, null, fill);
            }
        }

        private static void recordFill(
                final Bucket bucket,
                final long parentOrderId,
                final List<String> dedupeParentKeys,
                final ScenarioChildFill fill
        ) {
            if ((dedupeParentKeys == null || !dedupeParentKeys.contains(bucket.key))
                    && bucket.parentOrderIds.add(parentOrderId)) {
                bucket.parentOrderCount++;
            }
            if (dedupeParentKeys != null && !dedupeParentKeys.contains(bucket.key)) {
                dedupeParentKeys.add(bucket.key);
            }
            bucket.childOrderCount++;
            bucket.filledQty += fill.filledQty();
            bucket.notionalTicks += fill.notionalTicks();
            bucket.lastPriceTicks = fill.priceTicks();
            bucket.lastParentOrderId = parentOrderId;
        }

        synchronized String toJson() {
            return "{\"totals\":{\"parentOrderCount\":" + parentOrderCount
                    + ",\"childOrderCount\":" + childOrderCount
                    + ",\"filledQty\":" + filledQty
                    + ",\"remainingQty\":" + remainingQty
                    + ",\"notionalTicks\":" + notionalTicks
                    + ",\"averagePriceTicks\":" + averagePriceTicks(filledQty, notionalTicks) + "}"
                    + ",\"byInstrument\":" + bucketsToJson(byInstrument)
                    + ",\"byVenue\":" + bucketsToJson(byVenue)
                    + ",\"byInstrumentVenue\":" + bucketsToJson(byInstrumentVenue) + "}";
        }

        private static String bucketsToJson(final Map<String, Bucket> buckets) {
            return buckets.values().stream()
                    .sorted(Bucket::compareDisplayOrder)
                    .map(Bucket::toJson)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }

        private static long averagePriceTicks(final long qty, final long notional) {
            return qty == 0L ? 0L : notional / qty;
        }
    }

    private static final class Bucket {
        private final String key;
        private final int instrumentId;
        private final String instrumentSymbol;
        private final int venueId;
        private final String venueName;
        private final Set<Long> parentOrderIds = new HashSet<>();
        private long parentOrderCount;
        private long childOrderCount;
        private long filledQty;
        private long notionalTicks;
        private long lastPriceTicks;
        private long lastParentOrderId;

        private Bucket(
                final String key,
                final int instrumentId,
                final String instrumentSymbol,
                final int venueId,
                final String venueName
        ) {
            this.key = key;
            this.instrumentId = instrumentId;
            this.instrumentSymbol = instrumentSymbol;
            this.venueId = venueId;
            this.venueName = venueName;
        }

        static Bucket instrument(final ScenarioChildFill fill) {
            return new Bucket("instrument:" + fill.instrumentId(), fill.instrumentId(), fill.instrumentSymbol(), -1, "");
        }

        static Bucket venue(final ScenarioChildFill fill) {
            return new Bucket("venue:" + fill.venueId(), -1, "", fill.venueId(), fill.venueName());
        }

        static Bucket instrumentVenue(final ScenarioChildFill fill) {
            return new Bucket("instrumentVenue:" + fill.instrumentId() + ":" + fill.venueId(),
                    fill.instrumentId(), fill.instrumentSymbol(), fill.venueId(), fill.venueName());
        }

        static int compareDisplayOrder(final Bucket left, final Bucket right) {
            final int instrumentCompare = Integer.compare(left.instrumentId, right.instrumentId);
            if (instrumentCompare != 0) {
                return instrumentCompare;
            }
            return Integer.compare(left.venueId, right.venueId);
        }

        String toJson() {
            return "{"
                    + "\"instrumentId\":" + instrumentId
                    + ",\"instrumentSymbol\":\"" + escape(instrumentSymbol) + "\""
                    + ",\"venueId\":" + venueId
                    + ",\"venueName\":\"" + escape(venueName) + "\""
                    + ",\"parentOrderCount\":" + parentOrderCount
                    + ",\"childOrderCount\":" + childOrderCount
                    + ",\"filledQty\":" + filledQty
                    + ",\"notionalTicks\":" + notionalTicks
                    + ",\"averagePriceTicks\":" + CumulativeOrderStats.averagePriceTicks(filledQty, notionalTicks)
                    + ",\"lastPriceTicks\":" + lastPriceTicks
                    + ",\"lastParentOrderId\":" + lastParentOrderId + "}";
        }

        private static String escape(final String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
