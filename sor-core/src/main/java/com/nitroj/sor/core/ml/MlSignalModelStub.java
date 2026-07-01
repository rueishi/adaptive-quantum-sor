package com.nitroj.sor.core.ml;

import com.nitroj.sor.core.model.ModelSignalState;
import com.nitroj.sor.core.stats.FillQualityStats;
import com.nitroj.sor.core.stats.RegimeState;
import com.nitroj.sor.core.stats.SlippageStats;
import com.nitroj.sor.core.stats.ToxicityStats;
import com.nitroj.sor.core.stats.VenueStatsState;

/**
 * Responsibility: produce deterministic ML-style venue signals without ML runtime.
 *
 * <p>Role in system: Phase 1 needs model-shaped signals for optimizers while
 * explicitly avoiding Python, training, or non-deterministic inference in the
 * Java runtime.</p>
 *
 * <p>Relationships: reads feature stats from the aggregation layer and writes
 * {@link ModelSignalState} for policy optimization input.</p>
 *
 * <p>Lifecycle: called after stats aggregation. The stub is stateless and can
 * be reused for every optimizer cycle.</p>
 *
 * <p>Design intent: combine fill quality with toxicity, slippage, latency, and
 * regime penalties on a 0..10000 scale. Insufficient data remains neutral.</p>
 */
public final class MlSignalModelStub implements MlSignalModel {
    private final int instrumentCount;
    private final int venueCount;
    private final int regimeCount;

    public MlSignalModelStub(final int instrumentCount, final int venueCount, final int regimeCount) {
        if (instrumentCount <= 0 || venueCount <= 0 || regimeCount <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        this.instrumentCount = instrumentCount;
        this.venueCount = venueCount;
        this.regimeCount = regimeCount;
    }

    /**
     * Generates deterministic bounded scores. A venue with neutral/default
     * stats yields 5000; better fill quality raises the score while toxicity,
     * slippage, rejects, latency, and difficult regimes lower it.
     */
    @Override
    public ModelSignalState generate(
            final VenueStatsState venueStats,
            final FillQualityStats fillQualityStats,
            final ToxicityStats toxicityStats,
            final SlippageStats slippageStats,
            final RegimeState regimeState
    ) {
        if (venueStats == null || fillQualityStats == null || toxicityStats == null
                || slippageStats == null || regimeState == null) {
            throw new IllegalArgumentException("model inputs must not be null");
        }
        final ModelSignalState signals = new ModelSignalState(instrumentCount, venueCount, regimeCount);
        signals.setModelVersion(ModelSignalVersion.PHASE1_STUB_V1);
        for (int instrumentId = 0; instrumentId < instrumentCount; instrumentId++) {
            final int instrumentRegime = regimeState.regime(instrumentId);
            for (int venueId = 0; venueId < venueCount; venueId++) {
                for (int regimeId = 0; regimeId < regimeCount; regimeId++) {
                    final int idx = venueStats.idxIVR(instrumentId, venueId, regimeId);
                    int score = fillQualityStats.fillProbabilityBps(venueId);
                    score -= toxicityStats.toxicityBps(venueId) / 2;
                    score -= slippageStats.avgSlippageBps(venueId) / 2;
                    score -= venueStats.rejectRateBps[idx] / 2;
                    score -= Math.min(2_000, venueStats.latencyNanos[idx] / 1_000_000);
                    if (instrumentRegime == RegimeState.VOLATILE) {
                        score -= 250;
                    } else if (instrumentRegime == RegimeState.THIN_BOOK) {
                        score -= 500;
                    }
                    signals.setVenueScoreBps(instrumentId, venueId, regimeId, score);
                }
            }
        }
        return signals;
    }
}
