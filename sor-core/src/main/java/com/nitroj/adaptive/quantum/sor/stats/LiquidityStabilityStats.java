package com.nitroj.adaptive.quantum.sor.stats;

/**
 * Responsibility: store venue quote stability estimates.
 *
 * <p>Role in system: feature aggregation uses market data to express quote
 * fade and replenishment in a compact bps value for optimizer and ML layers.</p>
 *
 * <p>Relationships: complements queue and venue health stats when policy logic
 * ranks stable liquidity.</p>
 *
 * <p>Lifecycle: starts neutral and changes as simulated market data evolves.</p>
 *
 * <p>Design intent: bounded bps values keep downstream scoring deterministic.</p>
 */
public final class LiquidityStabilityStats {
    private final int[] stabilityBps;

    public LiquidityStabilityStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.stabilityBps = new int[venueCount];
        for (int i = 0; i < venueCount; i++) {
            stabilityBps[i] = 5_000;
        }
    }

    /** Sets bounded liquidity stability for one venue. */
    public void setStabilityBps(final int venueId, final int value) {
        checkVenue(venueId);
        stabilityBps[venueId] = ExecutionOutcomeStore.clampBps(value);
    }

    public int stabilityBps(final int venueId) {
        checkVenue(venueId);
        return stabilityBps[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= stabilityBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
