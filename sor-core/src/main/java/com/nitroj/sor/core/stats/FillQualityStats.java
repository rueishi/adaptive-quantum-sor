package com.nitroj.sor.core.stats;

/**
 * Responsibility: store fill probability and average fill size per venue.
 *
 * <p>Role in system: the feature layer turns venue outcomes into this compact
 * quality signal for ML and optimizer stubs.</p>
 *
 * <p>Relationships: values are copied into {@link VenueStatsState} for a
 * consolidated stats view.</p>
 *
 * <p>Lifecycle: starts neutral, then updates whenever outcome aggregation runs.</p>
 *
 * <p>Design intent: bps values are clamped to avoid invalid downstream scores.</p>
 */
public final class FillQualityStats {
    private final int[] fillProbabilityBps;
    private final long[] avgFillQty;

    public FillQualityStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.fillProbabilityBps = new int[venueCount];
        this.avgFillQty = new long[venueCount];
        for (int i = 0; i < venueCount; i++) {
            fillProbabilityBps[i] = 5_000;
        }
    }

    /** Updates bounded fill quality values for one venue. */
    public void update(final int venueId, final int fillProbabilityBps, final long avgFillQty) {
        checkVenue(venueId);
        this.fillProbabilityBps[venueId] = ExecutionOutcomeStore.clampBps(fillProbabilityBps);
        this.avgFillQty[venueId] = Math.max(0L, avgFillQty);
    }

    public int fillProbabilityBps(final int venueId) {
        checkVenue(venueId);
        return fillProbabilityBps[venueId];
    }

    public long avgFillQty(final int venueId) {
        checkVenue(venueId);
        return avgFillQty[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= fillProbabilityBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
