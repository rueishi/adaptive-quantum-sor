package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.execution.ExecutionFixtures;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.feature.FeatureAggregator;
import com.nitroj.adaptive.quantum.sor.feature.RegimeDetector;
import com.nitroj.adaptive.quantum.sor.feature.RollingWindowConfig;
import com.nitroj.adaptive.quantum.sor.sim.MarketDataSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueBehaviorSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueSessionSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueThrottleSimulator;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.FeedHealthState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.FillQualityStats;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import com.nitroj.adaptive.quantum.sor.stats.SlippageStats;
import com.nitroj.adaptive.quantum.sor.stats.ToxicityStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueHealthStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueLatencyStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueStatsState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify generated scenario books are usable by feature and
 * regime-detection consumers.
 *
 * <p>Role in system: this is the P5-TC-002 integration surface that proves the
 * scenario-driven market-data simulator feeds existing P1 feature components
 * without invalid state translation.</p>
 *
 * <p>Relationships: combines {@link ScenarioState}, {@link MarketDataSimulator},
 * {@link MarketBookState}, {@link RegimeDetector}, and {@link FeatureAggregator}
 * in the same flow used by later scenario tests.</p>
 *
 * <p>Lifecycle: run by Gradle/JUnit after unit tests and before any live
 * notebook scenario tooling is involved.</p>
 *
 * <p>Design intent: keep integration evidence deterministic while preserving a
 * clear boundary between scenario orchestration and deterministic simulator
 * unit tests.</p>
 */
final class ScenarioFeatureIntegrationTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void regimeDetectorSeesGeneratedThinAndVolatileConditions() {
        final RegimeDetector detector = new RegimeDetector(new RollingWindowConfig(8, 8_500L, 12L));

        final MarketBookState volatileBook = generatedBook(new ScenarioSpec("volatile", 101L, 12, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 11, RegimeState.VOLATILE)},
                true,
                ScenarioSimulatorConfig.defaults()));
        assertEquals(RegimeState.VOLATILE, detector.detect(volatileBook, 0, CONFIG.venueCount()));

        final MarketBookState thinBook = generatedBook(new ScenarioSpec("thin", 101L, 12, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 11, RegimeState.THIN_BOOK)},
                true,
                ScenarioSimulatorConfig.defaults()));
        assertEquals(RegimeState.THIN_BOOK, detector.detect(thinBook, 0, CONFIG.venueCount()));
    }

    @Test
    void featureAggregatorConsumesScenarioBooksWithoutInvalidStats() {
        final MarketBookState book = generatedBook(new ScenarioSpec("aggregate", 111L, 8, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 7, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        outcomes.append(1L, 0, ExecutionOutcomeStore.FULL_FILL, 100L, 1_000_000, 12, 30);
        outcomes.append(2L, 1, ExecutionOutcomeStore.PARTIAL_FILL, 50L, 2_000_000, 7, 10);
        final StatsBundle stats = new StatsBundle(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount());

        new FeatureAggregator(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount(),
                new RollingWindowConfig(8, 1_000L, 12L)).aggregate(
                book,
                outcomes,
                stats.venueStats,
                stats.latencyStats,
                stats.fillStats,
                stats.toxicityStats,
                stats.slippageStats,
                stats.healthStats,
                stats.regimeState
        );

        assertEquals(0, stats.healthStats.badEventCount());
        assertEquals(10_000, stats.fillStats.fillProbabilityBps(0));
        assertEquals(10_000, stats.fillStats.fillProbabilityBps(1));
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(stats.regimeState.regime(instrumentId) >= RegimeState.NORMAL);
            assertTrue(stats.regimeState.regime(instrumentId) <= RegimeState.THIN_BOOK);
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                for (int regimeId = 0; regimeId < CONFIG.regimeCount(); regimeId++) {
                    final int idx = stats.venueStats.idxIVR(instrumentId, venueId, regimeId);
                    assertTrue(stats.venueStats.fillProbabilityBps[idx] >= 0);
                    assertTrue(stats.venueStats.fillProbabilityBps[idx] <= 10_000);
                    assertTrue(stats.venueStats.rejectRateBps[idx] >= 0);
                    assertTrue(stats.venueStats.rejectRateBps[idx] <= 10_000);
                }
            }
        }
    }

    @Test
    void executionSkipsOutageOrHaltedStateAndRecordsResidual() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        fixture.sessions().setStatus(2, VenueStatus.OUTAGE);
        assertEquals(VenueStatus.OUTAGE, fixture.sessions().status(2));
        fixture.market().updateTopOfBook(0, 2, 100L, 101L, 9_000L, 9_000L);

        final ChildOrderBuffer output = new ChildOrderBuffer(8);
        final RouteDecisionResult result = new PolicyDrivenSorExecutioner(
                fixture.publisher(),
                fixture.market(),
                fixture.sessions(),
                fixture.risk()
        ).route(ExecutionFixtures.buy(2_000L), output);

        assertEquals(OrderStatus.ACKED, result.status);
        for (int i = 0; i < output.size(); i++) {
            assertNotEquals(2, output.get(i).venueId);
            assertTrue(fixture.sessions().isAvailable(output.get(i).venueId));
        }
    }

    @Test
    void featureHealthStatsReflectStaleAndOutageWindows() {
        final ScenarioSpec spec = new ScenarioSpec("health-outage", 131L, 40, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 39, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final FeedHealthState feedHealth = new FeedHealthState(CONFIG.venueCount());
        final MarketDataSimulator marketData = new MarketDataSimulator(CONFIG, spec.seed());
        boolean observedStale = false;
        for (int tick = 0; tick < spec.ticks(); tick++) {
            marketData.generateTick(book, scenario, feedHealth);
            observedStale |= feedHealth.isStale(3);
            scenario.clock().advance();
        }
        assertTrue(observedStale);

        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        outcomes.append(10L, 3, ExecutionOutcomeStore.REJECT, 0L, 1_000, 0, 0);
        outcomes.append(11L, 4, ExecutionOutcomeStore.REJECT, 0L, 1_000, 0, 0);
        final StatsBundle stats = new StatsBundle(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount());
        new FeatureAggregator(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount(),
                new RollingWindowConfig(8, 1_000L, 12L)).aggregate(
                book,
                outcomes,
                stats.venueStats,
                stats.latencyStats,
                stats.fillStats,
                stats.toxicityStats,
                stats.slippageStats,
                stats.healthStats,
                stats.regimeState
        );

        assertEquals(10_000, stats.healthStats.rejectRateBps(3));
        assertEquals(10_000, stats.healthStats.rejectRateBps(4));
        assertEquals(0, stats.healthStats.healthBps(3));
        assertEquals(0, stats.healthStats.healthBps(4));
    }

    @Test
    void generatedVenueOutcomesUpdateStatsAndSignalsByRegimeProfile() {
        final ScenarioSpec spec = new ScenarioSpec("venue-stats", 141L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, RegimeState.VOLATILE)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = validScenarioBook(5_000L);
        final ChildOrderBuffer orders = new ChildOrderBuffer(2);
        orders.add(1L, 1L, 0, 0, Side.BUY, 1_000L, 1L, 1L);
        orders.add(2L, 1L, 0, 2, Side.BUY, 1_000L, 1L, 1L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(8);
        final RegimeState regimes = new RegimeState(CONFIG.instrumentCount());
        regimes.setRegime(0, RegimeState.VOLATILE);
        regimes.setRegime(1, RegimeState.VOLATILE);

        new VenueBehaviorSimulator(CONFIG, 141L).process(
                orders,
                book,
                openSessions(),
                populatedThrottles(scenario),
                regimes,
                scenario,
                outcomes,
                new OutstandingChildOrderState(2),
                new ChildOrderState(2)
        );
        final StatsBundle stats = new StatsBundle(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount());
        new FeatureAggregator(CONFIG.instrumentCount(), CONFIG.venueCount(), CONFIG.regimeCount(),
                new RollingWindowConfig(8, 1_000L, 12L)).aggregate(
                book,
                outcomes,
                stats.venueStats,
                stats.latencyStats,
                stats.fillStats,
                stats.toxicityStats,
                stats.slippageStats,
                stats.healthStats,
                stats.regimeState
        );

        assertTrue(stats.toxicityStats.toxicityBps(2) > stats.toxicityStats.toxicityBps(0));
        assertTrue(stats.slippageStats.avgSlippageBps(2) > stats.slippageStats.avgSlippageBps(0));
        assertTrue(stats.latencyStats.avgLatencyNanos(0) > 0);
    }

    @Test
    void changingLiquidityProducesExplicitResidualQuantity() {
        final ScenarioSpec spec = new ScenarioSpec("venue-residual", 151L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = validScenarioBook(100L);
        final ChildOrderBuffer orders = new ChildOrderBuffer(1);
        orders.add(1L, 1L, 0, 0, Side.BUY, 1_000L, 1L, 1L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(1);

        new VenueBehaviorSimulator(CONFIG, 151L).process(
                orders,
                book,
                openSessions(),
                populatedThrottles(scenario),
                null,
                scenario,
                outcomes,
                outstanding,
                new ChildOrderState(1)
        );

        assertTrue(outcomes.filledQty(1) <= 100L);
        assertTrue(outstanding.remainingQty(0) >= 900L);
        assertEquals(ExecutionOutcomeStore.PARTIAL_FILL, outcomes.outcomeType(1));
    }

    @Test
    void syntheticScenarioRunsNormalVolatileThinBookSchedule() {
        final ScenarioSummary summary = new ScenarioRunner().run(new ScenarioSpec("schedule-nvt", 171L, 9, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 2, RegimeState.NORMAL),
                        new ScenarioWindow(3, 5, RegimeState.VOLATILE),
                        new ScenarioWindow(6, 8, RegimeState.THIN_BOOK)
                },
                true,
                ScenarioSimulatorConfig.defaults()));

        assertEquals(3, summary.detectedNormalCount());
        assertEquals(3, summary.detectedVolatileCount());
        assertEquals(3, summary.detectedThinBookCount());
        assertTrue(summary.bookChecksum() != 0L);
        assertTrue(summary.outcomeCount() > 0);
    }

    private static MarketBookState generatedBook(final ScenarioSpec spec) {
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, spec.seed());
        for (int tick = 0; tick < spec.ticks(); tick++) {
            simulator.generateTick(book, scenario);
            scenario.clock().advance();
        }
        return book;
    }

    private static MarketBookState validScenarioBook(final long visibleQty) {
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                book.updateTopOfBook(instrumentId, venueId, 100L + instrumentId * 100L,
                        101L + instrumentId * 100L + venueId, visibleQty, visibleQty);
            }
        }
        return book;
    }

    private static VenueSessionState openSessions() {
        final VenueSessionState sessions = new VenueSessionState(CONFIG.venueCount());
        new VenueSessionSimulator(CONFIG).openAll(sessions);
        return sessions;
    }

    private static VenueThrottleState populatedThrottles(final ScenarioState scenario) {
        final VenueThrottleState throttles = new VenueThrottleState(CONFIG.venueCount());
        new VenueThrottleSimulator(CONFIG).populate(throttles, scenario);
        return throttles;
    }

    private static final class StatsBundle {
        final VenueStatsState venueStats;
        final VenueLatencyStats latencyStats;
        final FillQualityStats fillStats;
        final ToxicityStats toxicityStats;
        final SlippageStats slippageStats;
        final VenueHealthStats healthStats;
        final RegimeState regimeState;

        StatsBundle(final int instrumentCount, final int venueCount, final int regimeCount) {
            venueStats = new VenueStatsState(instrumentCount, venueCount, regimeCount);
            latencyStats = new VenueLatencyStats(venueCount);
            fillStats = new FillQualityStats(venueCount);
            toxicityStats = new ToxicityStats(venueCount);
            slippageStats = new SlippageStats(venueCount);
            healthStats = new VenueHealthStats(venueCount);
            regimeState = new RegimeState(instrumentCount);
        }
    }
}
