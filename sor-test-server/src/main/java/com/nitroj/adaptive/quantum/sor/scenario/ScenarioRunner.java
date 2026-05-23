package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.sim.adapters.InMemoryPersistence;
import com.nitroj.sor.sim.adapters.ManualClock;
import com.nitroj.sor.sim.scenario.SimChildOrder;
import com.nitroj.sor.sim.scenario.SimConfig;
import com.nitroj.sor.sim.scenario.SimMarketSessionSnapshot;
import com.nitroj.sor.sim.scenario.SimParentOrder;
import com.nitroj.sor.sim.scenario.venues.SimVenueOutcome;
import com.nitroj.sor.sim.scenario.venues.SimVenueSessionSnapshot;
import com.nitroj.sor.sim.scenario.venues.SimVenueThrottleSnapshot;
import com.nitroj.sor.sim.scenario.SimulatedClusterController;
import com.nitroj.sor.sim.scenario.SimulatedFeeSchedule;
import com.nitroj.sor.sim.scenario.SimulatedInstrumentCatalog;
import com.nitroj.sor.sim.adapters.SimulatedMarketDataSource;
import com.nitroj.sor.sim.scenario.SimulatedOrderInjector;
import com.nitroj.sor.sim.adapters.SimulatedRiskProvider;
import com.nitroj.sor.sim.adapters.SimulatedVenueAdapter;
import com.nitroj.sor.sim.scenario.SimulatedVenueCatalog;

import java.util.List;

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
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        final RunEvidence result = runWithSimulators(spec);
        final int normal = countRegime(spec, 0);
        final int volatileCount = countRegime(spec, 1);
        final int thinBook = countRegime(spec, 2);
        return new ScenarioSummary(
                spec.scenarioId(),
                spec.seed(),
                spec.ticks(),
                result.replayChecksum(),
                result.orderCount(),
                result.routeDecidedEvents(),
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
        try (SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(1024, 100))
                .marketData(marketData)
                .venueAdapter(venueAdapter)
                .riskProvider(risk)
                .persistence(new InMemoryPersistence())
                .clock(clock)
                .build()) {
            engine.warmup(0);
        }

        int orderCount = 0;
        int fillEvents = 0;
        int rejectEvents = 0;
        long residual = 0L;
        long checksum = spec.seed() ^ spec.scenarioId().hashCode();
        final SimMarketSessionSnapshot marketSessions = marketData.openAllSessions();
        final SimVenueSessionSnapshot venueSessions = venueAdapter.openAllSessions();
        final SimVenueThrottleSnapshot throttles = venueAdapter.populateThrottle();
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
                final int venueId = Math.floorMod(tick, config.venueCount());
                final long childQty = Math.min(parent.quantity(), 2_000L);
                final SimChildOrder child = new SimChildOrder(parent.parentOrderId(), parent.parentOrderId(),
                        parent.instrumentId(), venueId, parent.side(), childQty, 1L, clock.epochNanos());
                for (SimVenueOutcome outcome : venueAdapter.process(child, marketData.currentBook(), venueSessions, throttles, regime)) {
                    checksum = checksum * 31L + outcome.type().hashCode() + outcome.quantity();
                    if ("REJECT".equals(outcome.type())) {
                        rejectEvents++;
                        residual += childQty;
                    } else if (outcome.type().endsWith("FILL")) {
                        fillEvents++;
                        residual += Math.max(0L, childQty - outcome.quantity());
                    }
                }
            }
            checksum = checksum * 31L + regime + marketData.currentMidTicks(0);
            clock.advanceNanos(1_000_000L);
        }
        return new RunEvidence(orderCount, orderCount, fillEvents, rejectEvents, residual, checksum);
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
                               long residualQuantity, long replayChecksum) {
    }
}
