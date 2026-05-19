package com.nitroj.adaptive.quantum.sor.feature;

import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.FillQualityStats;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import com.nitroj.adaptive.quantum.sor.stats.SlippageStats;
import com.nitroj.adaptive.quantum.sor.stats.ToxicityStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueHealthStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueLatencyStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueStatsState;

/**
 * Responsibility: convert raw market and execution outcomes into rolling stats.
 *
 * <p>Role in system: this is the Phase 1 feature aggregation layer that feeds
 * ML stubs, optimizers, policy compilation, and regime-aware routing.</p>
 *
 * <p>Relationships: reads {@link ExecutionOutcomeStore} and {@link MarketBookState};
 * writes the individual stats structures plus consolidated {@link VenueStatsState}
 * and {@link RegimeState}.</p>
 *
 * <p>Lifecycle: constructed with fixed dimensions and reused for periodic
 * aggregation passes. It does not own the input or output state.</p>
 *
 * <p>Design intent: neutral defaults are emitted when data is missing, while bad
 * outcome references are counted and ignored instead of poisoning stats.</p>
 */
public final class FeatureAggregator {
    private final int instrumentCount;
    private final int venueCount;
    private final int regimeCount;
    private final RollingWindowConfig config;
    private final RegimeDetector regimeDetector;

    public FeatureAggregator(
            final int instrumentCount,
            final int venueCount,
            final int regimeCount,
            final RollingWindowConfig config
    ) {
        if (instrumentCount <= 0 || venueCount <= 0 || regimeCount <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.instrumentCount = instrumentCount;
        this.venueCount = venueCount;
        this.regimeCount = regimeCount;
        this.config = config;
        this.regimeDetector = new RegimeDetector(config);
    }

    /**
     * Aggregates the current outcome store and market book into stats outputs.
     */
    public void aggregate(
            final MarketBookState marketBookState,
            final ExecutionOutcomeStore outcomes,
            final VenueStatsState venueStats,
            final VenueLatencyStats latencyStats,
            final FillQualityStats fillQualityStats,
            final ToxicityStats toxicityStats,
            final SlippageStats slippageStats,
            final VenueHealthStats healthStats,
            final RegimeState regimeState
    ) {
        if (marketBookState == null || outcomes == null || venueStats == null || latencyStats == null
                || fillQualityStats == null || toxicityStats == null || slippageStats == null
                || healthStats == null || regimeState == null) {
            throw new IllegalArgumentException("aggregation inputs must not be null");
        }

        final int[] eventCount = new int[venueCount];
        final int[] fillCount = new int[venueCount];
        final int[] rejectCount = new int[venueCount];
        final long[] fillQty = new long[venueCount];
        final long[] latency = new long[venueCount];
        final long[] slippage = new long[venueCount];
        final long[] toxicity = new long[venueCount];

        final int start = Math.max(0, outcomes.size() - config.maxOutcomeWindow);
        for (int i = start; i < outcomes.size(); i++) {
            final int venueId = outcomes.venueId(i);
            if (venueId < 0 || venueId >= venueCount || outcomes.childOrderId(i) <= 0) {
                healthStats.recordBadEvent();
                outcomes.recordWarning();
                continue;
            }
            eventCount[venueId]++;
            latency[venueId] += outcomes.latencyNanos(i);
            slippage[venueId] += outcomes.slippageBps(i);
            toxicity[venueId] += outcomes.toxicityBps(i);
            final int type = outcomes.outcomeType(i);
            if (ExecutionOutcomeStore.isFill(type)) {
                fillCount[venueId]++;
                fillQty[venueId] += outcomes.filledQty(i);
            } else if (ExecutionOutcomeStore.isReject(type)) {
                rejectCount[venueId]++;
            }
        }

        for (int venueId = 0; venueId < venueCount; venueId++) {
            final int count = eventCount[venueId];
            final int fillProbability = count == 0 ? 5_000 : (int) ((long) fillCount[venueId] * 10_000L / count);
            final int rejectRate = count == 0 ? 0 : (int) ((long) rejectCount[venueId] * 10_000L / count);
            final int avgLatency = count == 0 ? 0 : boundedAverage(latency[venueId], count);
            final int avgSlippage = count == 0 ? 0 : boundedAverage(slippage[venueId], count);
            final int avgToxicity = count == 0 ? 0 : boundedAverage(toxicity[venueId], count);
            final long avgFillQty = fillCount[venueId] == 0 ? 0L : fillQty[venueId] / fillCount[venueId];

            latencyStats.setAvgLatencyNanos(venueId, avgLatency);
            fillQualityStats.update(venueId, fillProbability, avgFillQty);
            toxicityStats.setToxicityBps(venueId, avgToxicity);
            slippageStats.setAvgSlippageBps(venueId, avgSlippage);
            healthStats.update(venueId, rejectRate);
        }

        for (int instrumentId = 0; instrumentId < instrumentCount; instrumentId++) {
            final int regime = regimeDetector.detect(marketBookState, instrumentId, venueCount);
            regimeState.setRegime(instrumentId, regime);
            for (int venueId = 0; venueId < venueCount; venueId++) {
                for (int regimeId = 0; regimeId < regimeCount; regimeId++) {
                    venueStats.update(
                            instrumentId,
                            venueId,
                            regimeId,
                            latencyStats.avgLatencyNanos(venueId),
                            fillQualityStats.fillProbabilityBps(venueId),
                            toxicityStats.toxicityBps(venueId),
                            healthStats.rejectRateBps(venueId),
                            0,
                            slippageStats.avgSlippageBps(venueId)
                    );
                }
            }
        }
    }

    private static int boundedAverage(final long total, final int count) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, total / count));
    }
}
