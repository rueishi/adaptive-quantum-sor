package com.nitroj.sor.core.stats;

import com.nitroj.sor.core.util.Indexing;

/**
 * Responsibility: provide the consolidated venue feature view in IVR layout.
 *
 * <p>Role in system: optimizers and policy compilation can read one compact
 * stats object rather than many venue-specific feature structures.</p>
 *
 * <p>Relationships: {@code FeatureAggregator} writes this from latency, fill,
 * toxicity, slippage, health, fee, and market-impact inputs.</p>
 *
 * <p>Lifecycle: allocated per instrument/venue/regime dimension set and updated
 * in place as rolling stats change.</p>
 *
 * <p>Design intent: neutral defaults avoid NaN and unavailable-data behavior
 * while preserving the spec's primitive array layout.</p>
 */
public final class VenueStatsState {
    public final int instrumentCount;
    public final int venueCount;
    public final int regimeCount;

    public final int[] latencyNanos;
    public final int[] fillProbabilityBps;
    public final int[] toxicityBps;
    public final int[] rejectRateBps;
    public final int[] feePenaltyTicks;
    public final int[] marketImpactBps;

    private final Indexing indexing;
    private long sequence;

    public VenueStatsState(final int instrumentCount, final int venueCount, final int regimeCount) {
        this.indexing = new Indexing(instrumentCount, venueCount, regimeCount, 1);
        this.instrumentCount = instrumentCount;
        this.venueCount = venueCount;
        this.regimeCount = regimeCount;
        final int length = instrumentCount * venueCount * regimeCount;
        this.latencyNanos = new int[length];
        this.fillProbabilityBps = new int[length];
        this.toxicityBps = new int[length];
        this.rejectRateBps = new int[length];
        this.feePenaltyTicks = new int[length];
        this.marketImpactBps = new int[length];
        for (int i = 0; i < length; i++) {
            fillProbabilityBps[i] = 5_000;
        }
    }

    /**
     * Updates a full IVR stats cell with bounded bps values.
     */
    public void update(
            final int instrumentId,
            final int venueId,
            final int regimeId,
            final int latencyNanos,
            final int fillProbabilityBps,
            final int toxicityBps,
            final int rejectRateBps,
            final int feePenaltyTicks,
            final int marketImpactBps
    ) {
        final int idx = indexing.idxIVR(instrumentId, venueId, regimeId);
        this.latencyNanos[idx] = Math.max(0, latencyNanos);
        this.fillProbabilityBps[idx] = ExecutionOutcomeStore.clampBps(fillProbabilityBps);
        this.toxicityBps[idx] = ExecutionOutcomeStore.clampBps(toxicityBps);
        this.rejectRateBps[idx] = ExecutionOutcomeStore.clampBps(rejectRateBps);
        this.feePenaltyTicks[idx] = feePenaltyTicks;
        this.marketImpactBps[idx] = ExecutionOutcomeStore.clampBps(marketImpactBps);
        sequence++;
    }

    public int idxIVR(final int instrumentId, final int venueId, final int regimeId) {
        return indexing.idxIVR(instrumentId, venueId, regimeId);
    }

    public long sequence() {
        return sequence;
    }
}
