package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.api.MarketDataSnapshotSummary;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.api.SorLifecycleEventTypes;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import com.nitroj.sor.testkit.sim.adapters.InMemoryPersistence;
import com.nitroj.sor.testkit.sim.adapters.ManualClock;
import com.nitroj.sor.testkit.sim.scenario.SimConfig;
import com.nitroj.sor.testkit.sim.scenario.SimMarketSessionSnapshot;
import com.nitroj.sor.testkit.sim.scenario.SimParentOrder;
import com.nitroj.sor.testkit.sim.scenario.venues.SimVenueSessionSnapshot;
import com.nitroj.sor.testkit.sim.scenario.venues.SimVenueThrottleSnapshot;
import com.nitroj.sor.testkit.sim.scenario.SimulatedClusterController;
import com.nitroj.sor.testkit.sim.scenario.SimulatedFeeSchedule;
import com.nitroj.sor.testkit.sim.scenario.SimulatedInstrumentCatalog;
import com.nitroj.sor.testkit.sim.adapters.SimulatedMarketDataSource;
import com.nitroj.sor.testkit.sim.scenario.SimulatedOrderInjector;
import com.nitroj.sor.testkit.sim.adapters.SimulatedRiskProvider;
import com.nitroj.sor.testkit.sim.adapters.SimulatedVenueAdapter;
import com.nitroj.sor.testkit.sim.scenario.SimulatedVenueCatalog;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsibility: execute a replayable scenario specification.
 *
 * <p>Role in system: this P8-28 implementation preserves the established
 * {@link ScenarioSummary} surface while composing SPI-backed simulator
 * components from {@code sor-test-server} instead of production imports from the
 * legacy simulator package.</p>
 *
 * <p>Relationships: adapts core {@link ScenarioSpec} values into simulator
 * component configuration, composes the simulator SPI implementations, and maps
 * deterministic evidence back to the existing summary and optimizer lineage
 * APIs.</p>
 *
 * <p>Lifecycle: stateless runner created by tests, CLI, API handlers, or
 * notebooks for each scenario replay.</p>
 *
 * <p>Design intent: keep public scenario behavior stable while making the new
 * simulator path the only production replay implementation.</p>
 */
public final class ScenarioRunner {
    /**
     * Runs the scenario through simulator components and maps deterministic
     * route/fill/reject evidence into the legacy-compatible summary record.
     */
    public ScenarioSummary run(final ScenarioSpec spec) {
        return runWithEvidence(spec).summary();
    }

    /**
     * Runs the scenario and returns replay/audit evidence linked to the public
     * summary.
     */
    public ScenarioAuditEvidence runWithEvidence(final ScenarioSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        final RunEvidence result = runWithSimulators(spec);
        final ScenarioSummary summary = summary(spec, result);
        return new ScenarioAuditEvidence(
                summary,
                result.resetSummary(),
                result.marketDataSnapshot(),
                result.finalStateSummary(),
                result.routeDecidedEvents(),
                result.childOrderCount(),
                result.fillEvents(),
                result.rejectEvents(),
                result.lifecycleEventCount());
    }

    private static ScenarioSummary summary(final ScenarioSpec spec, final RunEvidence result) {
        final int normal = countRegime(spec, 0);
        final int volatileCount = countRegime(spec, 1);
        final int thinBook = countRegime(spec, 2);
        return new ScenarioSummary(
                spec.scenarioId(),
                spec.seed(),
                spec.ticks(),
                result.replayChecksum() ^ result.marketDataSnapshot().checksum(),
                result.orderCount(),
                result.childOrderCount(),
                result.fillEvents() + result.rejectEvents(),
                result.routeDecidedEvents(),
                result.rejectEvents(),
                Math.max(0, result.fillEvents() - result.orderCount()),
                Math.min(result.fillEvents(), result.orderCount()),
                result.residualQuantity(),
                normal,
                volatileCount * 2L,
                spec.simulatorConfig().normalDisplayedQty(),
                spec.simulatorConfig().thinBookDisplayedQty(),
                0,
                0,
                normal,
                volatileCount,
                thinBook
        );
    }

    /**
     * Builds compact optimizer input lineage from a completed scenario summary.
     *
     * <p>If simulator health is unhealthy, this method returns {@code null} so
     * callers cannot accidentally publish optimizer inputs from partial state.</p>
     */
    public PolicyOptimizationInput optimizerInputLineage(
            final ScenarioSummary summary,
            final ScenarioSpec spec,
            final Object simulatorHealth
    ) {
        if (summary == null || spec == null || simulatorHealth == null) {
            throw new IllegalArgumentException("summary, spec, and simulatorHealth must not be null");
        }
        if (!isHealthy(simulatorHealth)) {
            return null;
        }
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = Math.abs(summary.bookChecksum());
        input.createdAtNanos = summary.ticksRun();
        input.instrumentCount = spec.config().instrumentCount();
        input.venueCount = spec.config().venueCount();
        input.regimeCount = spec.config().regimeCount();
        input.urgencyCount = spec.config().urgencyCount();
        input.marketDataSnapshotSeq = summary.bookChecksum();
        input.executionOutcomeSnapshotSeq = summary.outcomeCount();
        input.scenarioId = summary.scenarioId();
        input.scenarioSeed = summary.seed();
        input.scenarioTicks = summary.ticksRun();
        input.scenarioStartTickInclusive = 0;
        input.scenarioEndTickInclusive = Math.max(0, summary.ticksRun() - 1);
        input.scenarioInputPublishable = true;
        return input;
    }

    private static boolean isHealthy(final Object simulatorHealth) {
        try {
            return (Boolean) simulatorHealth.getClass().getMethod("healthy").invoke(simulatorHealth);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("simulatorHealth must expose healthy()", ex);
        }
    }

    private static RunEvidence runWithSimulators(final ScenarioSpec spec) {
        final SimConfig config = new SimConfig(spec.config().instrumentCount(), spec.config().venueCount(),
                spec.config().regimeCount(), spec.config().urgencyCount());
        final ManualClock clock = new ManualClock(1_000L);
        final SimulatedMarketDataSource marketData = new SimulatedMarketDataSource(config, spec.seed());
        final SimulatedVenueAdapter venueAdapter = new SimulatedVenueAdapter(clock, config, spec.seed() ^ 0x56424cL);
        final SimulatedRiskProvider risk = new SimulatedRiskProvider();
        risk.generate(config);
        final SimulatedOrderInjector injector = new SimulatedOrderInjector(config, spec.seed() ^ 0x504f524445524cL);
        final SimulatedClusterController cluster = new SimulatedClusterController(clock, marketData, venueAdapter);
        new SimulatedFeeSchedule().generate(config);
        new SimulatedInstrumentCatalog().generate(config);
        new SimulatedVenueCatalog().generate(config);
        final AtomicInteger routeEvents = new AtomicInteger();
        final AtomicInteger childEvents = new AtomicInteger();
        final AtomicInteger fillEvents = new AtomicInteger();
        final AtomicInteger rejectEvents = new AtomicInteger();
        int orderCount = 0;
        long residual = 0L;
        long checksum = spec.seed() ^ spec.scenarioId().hashCode();
        final InMemoryPersistence persistence = new InMemoryPersistence();
        final SimMarketSessionSnapshot marketSessions = marketData.openAllSessions();
        final SimVenueSessionSnapshot venueSessions = venueAdapter.openAllSessions();
        final SimVenueThrottleSnapshot throttles = venueAdapter.populateThrottle();
        try (SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(1024, 100, config.instrumentCount(), config.venueCount(), true))
                .marketData(marketData)
                .venueAdapter(venueAdapter)
                .riskProvider(risk)
                .persistence(persistence)
                .clock(clock)
                .build()) {
            final SorControlPlane controlPlane = (SorControlPlane) engine;
            final SorResetSummary resetSummary = controlPlane.reset(new SorResetRequest(
                    SorResetMode.SCENARIO_REPLAY_RESET, false, "scenario replay " + spec.scenarioId()));
            engine.registerListener(event -> {
                if (event instanceof SorEvent.RouteDecided) {
                    routeEvents.incrementAndGet();
                } else if (event instanceof SorEvent.ChildOrderEmitted) {
                    childEvents.incrementAndGet();
                } else if (event instanceof SorEvent.Filled) {
                    fillEvents.incrementAndGet();
                } else if (event instanceof SorEvent.Rejected) {
                    rejectEvents.incrementAndGet();
                }
            });
            engine.warmup(0);
            for (int tick = 0; tick < spec.ticks(); tick++) {
                final int regime = spec.regimeAt(tick);
                final int currentTick = tick;
                cluster.runGuarded("marketData", () -> marketData.generateTick(regime, currentTick));
                marketData.applyMarketSession(marketSessions, regime, tick);
                venueAdapter.applyVenueSession(venueSessions, regime, tick);
                venueAdapter.populateThrottle(throttles, regime);
                final List<SimParentOrder> parents = injector.ordersForTick(tick, regime, clock.epochNanos(), List.of());
                orderCount += parents.size();
                for (SimParentOrder parent : parents) {
                    final long parentOrderId = engine.submitParentOrder(ParentOrderRequest.builder()
                            .instrumentId(parent.instrumentId())
                            .side(parent.side())
                            .quantity(parent.quantity())
                            .urgency(0)
                            .clientOrderId(parent.parentOrderId())
                            .arrivalEpochNanos(clock.epochNanos())
                            .build());
                    venueAdapter.pollVenue(0, 1);
                    final OrderStatus status = engine.getOrderStatus(parentOrderId).orElseThrow();
                    residual += status.remainingQuantity();
                    checksum = checksum * 31L + status.status().ordinal() + status.filledQuantity();
                }
                checksum = checksum * 31L + regime + marketData.currentMidTicks(0);
                clock.advanceNanos(1_000_000L);
            }
            final MarketDataSnapshotSummary marketDataSnapshot = controlPlane.marketDataSnapshot();
            final SorStateSummary finalStateSummary = controlPlane.stateSummary();
            final LifecycleCounts lifecycleCounts = countLifecycleEvents(persistence);
            return new RunEvidence(orderCount, lifecycleCounts.routeEvents(), lifecycleCounts.fillEvents(),
                    lifecycleCounts.rejectEvents(), residual, checksum, lifecycleCounts.childEvents(),
                    resetSummary, marketDataSnapshot, finalStateSummary, lifecycleCounts.totalEvents());
        }
    }

    private static LifecycleCounts countLifecycleEvents(final InMemoryPersistence persistence) {
        long count = 0L;
        int routeEvents = 0;
        int childEvents = 0;
        int fillEvents = 0;
        int rejectEvents = 0;
        final java.util.Iterator<com.nitroj.sor.api.spi.LifecycleEvent> replay = persistence.replay(0);
        while (replay.hasNext()) {
            final com.nitroj.sor.api.spi.LifecycleEvent event = replay.next();
            count++;
            if (event.eventType() == SorLifecycleEventTypes.ROUTE_DECIDED) {
                routeEvents++;
            } else if (event.eventType() == SorLifecycleEventTypes.CHILD_ORDER_EMITTED) {
                childEvents++;
            } else if (event.eventType() == SorLifecycleEventTypes.FILL_DELIVERED) {
                fillEvents++;
            } else if (event.eventType() == SorLifecycleEventTypes.REJECT_DELIVERED) {
                rejectEvents++;
            }
        }
        return new LifecycleCounts(count, routeEvents, childEvents, fillEvents, rejectEvents);
    }

    private static int countRegime(final ScenarioSpec spec, final int regimeId) {
        int count = 0;
        for (int tick = 0; tick < spec.ticks(); tick++) {
            if (spec.regimeAt(tick) == regimeId) {
                count++;
            }
        }
        return count;
    }

    private record RunEvidence(int orderCount, int routeDecidedEvents, int fillEvents, int rejectEvents,
                               long residualQuantity, long replayChecksum, int childOrderCount,
                               SorResetSummary resetSummary,
                               MarketDataSnapshotSummary marketDataSnapshot,
                               SorStateSummary finalStateSummary,
                               long lifecycleEventCount) {
    }

    private record LifecycleCounts(long totalEvents, int routeEvents, int childEvents, int fillEvents,
                                   int rejectEvents) {
    }
}
