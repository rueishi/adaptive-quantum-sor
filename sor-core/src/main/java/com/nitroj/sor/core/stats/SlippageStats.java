package com.nitroj.sor.core.stats;

/**
 * Responsibility: store bounded slippage estimates per venue.
 *
 * <p>Role in system: feature aggregation derives this from simulated arrival
 * price versus fill price behavior and ML/optimizer code reads it as a cost
 * signal.</p>
 *
 * <p>Relationships: complements toxicity and market impact signals in the
 * policy model input surface.</p>
 *
 * <p>Lifecycle: starts neutral and is updated by each aggregation pass.</p>
 *
 * <p>Design intent: bps clamping avoids invalid policy penalties.</p>
 */
public final class SlippageStats {
    private final int[] avgSlippageBps;

    public SlippageStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.avgSlippageBps = new int[venueCount];
    }

    /** Sets bounded average slippage for one venue. */
    public void setAvgSlippageBps(final int venueId, final int value) {
        checkVenue(venueId);
        avgSlippageBps[venueId] = ExecutionOutcomeStore.clampBps(value);
    }

    public int avgSlippageBps(final int venueId) {
        checkVenue(venueId);
        return avgSlippageBps[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= avgSlippageBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
