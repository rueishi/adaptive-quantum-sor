package com.nitroj.sor.core.stats;

import com.nitroj.sor.core.feature.FeatureAggregator;
import com.nitroj.sor.core.feature.RegimeDetector;
import com.nitroj.sor.core.feature.RollingWindowConfig;
import com.nitroj.sor.core.state.MarketBookState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify execution outcome and feature stats structures.
 *
 * <p>Role in system: covers P1-TC-008 and P1-TC-009 behavior for default
 * neutral stats, outcome-driven updates, malformed event handling, and regime
 * detection.</p>
 *
 * <p>Relationships: exercises stats structures through {@link FeatureAggregator}
 * because that is the system component that mutates them from raw events.</p>
 *
 * <p>Lifecycle: executed by unit tests whenever stats or feature code changes.</p>
 *
 * <p>Design intent: ensure invalid bps/latency values are bounded and no-data
 * scenarios remain deterministic rather than NaN-like.</p>
 */
final class StatsAndFeatureAggregationTest {
    @Test
    void defaultStatsAreValidAndBounded() {
        final VenueStatsState venueStats = new VenueStatsState(1, 2, 3);
        final FillQualityStats fillStats = new FillQualityStats(2);
        final QueueStats queueStats = new QueueStats(2);
        final LiquidityStabilityStats liquidityStats = new LiquidityStabilityStats(2);
        final VenueHealthStats healthStats = new VenueHealthStats(2);
        final RegimeState regimeState = new RegimeState(1);

        assertEquals(5_000, venueStats.fillProbabilityBps[venueStats.idxIVR(0, 0, 0)]);
        assertEquals(5_000, fillStats.fillProbabilityBps(0));
        assertEquals(5_000, queueStats.queueSurvivalBps(0));
        assertEquals(5_000, liquidityStats.stabilityBps(0));
        assertEquals(10_000, healthStats.healthBps(0));
        assertEquals(RegimeState.NORMAL, regimeState.regime(0));
    }

    @Test
    void fillAndRejectOutcomesAffectQualityAndHealthStats() {
        final MarketBookState book = validBook(1, 2, 100L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(8);
        outcomes.append(1L, 0, ExecutionOutcomeStore.FULL_FILL, 100L, 1_000_000, 10, 20);
        outcomes.append(2L, 1, ExecutionOutcomeStore.REJECT, 0L, 2_000_000, 0, 0);

        final StatsBundle stats = new StatsBundle(1, 2, 3);
        aggregator().aggregate(book, outcomes, stats.venueStats, stats.latencyStats, stats.fillStats,
                stats.toxicityStats, stats.slippageStats, stats.healthStats, stats.regimeState);

        assertEquals(10_000, stats.fillStats.fillProbabilityBps(0));
        assertEquals(100L, stats.fillStats.avgFillQty(0));
        assertEquals(10_000, stats.healthStats.rejectRateBps(1));
        assertEquals(0, stats.healthStats.healthBps(1));
        assertEquals(1_000_000, stats.latencyStats.avgLatencyNanos(0));
        assertEquals(20, stats.toxicityStats.toxicityBps(0));
        assertEquals(10, stats.slippageStats.avgSlippageBps(0));
        assertEquals(10_000, stats.venueStats.fillProbabilityBps[stats.venueStats.idxIVR(0, 0, 0)]);
    }

    @Test
    void malformedOutcomeReferencesAreIgnoredWithWarningCounter() {
        final MarketBookState book = validBook(1, 2, 100L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        outcomes.append(1L, 9, ExecutionOutcomeStore.FULL_FILL, 100L, 1_000, 0, 0);
        final StatsBundle stats = new StatsBundle(1, 2, 3);

        aggregator().aggregate(book, outcomes, stats.venueStats, stats.latencyStats, stats.fillStats,
                stats.toxicityStats, stats.slippageStats, stats.healthStats, stats.regimeState);

        assertEquals(1, stats.healthStats.badEventCount());
        assertEquals(1, outcomes.warningCount());
        assertEquals(5_000, stats.fillStats.fillProbabilityBps(0));
    }

    @Test
    void zeroDataProducesNeutralStatsAndNormalRegime() {
        final MarketBookState book = validBook(1, 2, 1_000L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        final StatsBundle stats = new StatsBundle(1, 2, 3);

        aggregator().aggregate(book, outcomes, stats.venueStats, stats.latencyStats, stats.fillStats,
                stats.toxicityStats, stats.slippageStats, stats.healthStats, stats.regimeState);

        assertEquals(5_000, stats.fillStats.fillProbabilityBps(0));
        assertEquals(0, stats.healthStats.rejectRateBps(0));
        assertEquals(RegimeState.NORMAL, stats.regimeState.regime(0));
    }

    @Test
    void volatileAndThinLiquidityScenariosChangeRegime() {
        final RegimeDetector detector = new RegimeDetector(new RollingWindowConfig(10, 100, 10));
        final MarketBookState volatileBook = validBook(1, 1, 1_000L);
        volatileBook.updateTopOfBook(0, 0, 100, 130, 1_000, 1_000);
        assertEquals(RegimeState.VOLATILE, detector.detect(volatileBook, 0, 1));

        final MarketBookState thinBook = validBook(1, 1, 10L);
        assertEquals(RegimeState.THIN_BOOK, detector.detect(thinBook, 0, 1));
    }

    @Test
    void invalidAndBoundaryValuesAreClampedOrRejected() {
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(1);
        outcomes.append(1L, 0, ExecutionOutcomeStore.FULL_FILL, 1L, -1, 20_000, -5);
        assertEquals(0, outcomes.latencyNanos(0));
        assertEquals(10_000, outcomes.slippageBps(0));
        assertEquals(0, outcomes.toxicityBps(0));

        assertEquals("outcomeType must be known", assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutionOutcomeStore(1).append(1L, 0, 99, 0L, 0, 0, 0)
        ).getMessage());
        assertThrows(IndexOutOfBoundsException.class, () -> new RegimeState(1).regime(1));
        assertThrows(IllegalArgumentException.class, () -> new RollingWindowConfig(0, 0, 0));
    }

    private static FeatureAggregator aggregator() {
        return new FeatureAggregator(1, 2, 3, new RollingWindowConfig(10, 100, 10));
    }

    private static MarketBookState validBook(final int instruments, final int venues, final long qty) {
        final MarketBookState book = new MarketBookState(instruments, venues);
        for (int instrumentId = 0; instrumentId < instruments; instrumentId++) {
            for (int venueId = 0; venueId < venues; venueId++) {
                book.updateTopOfBook(instrumentId, venueId, 100, 101, qty, qty);
            }
        }
        return book;
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
