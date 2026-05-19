package com.nitroj.adaptive.quantum.sor.stats;

/**
 * Responsibility: store bounded latency statistics per venue.
 *
 * <p>Role in system: feature aggregation writes simulated venue latency and ML
 * and policy layers read it as an execution-quality signal.</p>
 *
 * <p>Relationships: {@link VenueStatsState} mirrors the average latency field
 * for consolidated optimizer reads.</p>
 *
 * <p>Lifecycle: allocated per stats snapshot and updated by the feature layer.</p>
 *
 * <p>Design intent: neutral defaults avoid NaN/Infinity when no outcomes exist.</p>
 */
public final class VenueLatencyStats {
    private final int[] avgLatencyNanos;

    public VenueLatencyStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.avgLatencyNanos = new int[venueCount];
    }

    /** Sets a non-negative latency value. */
    public void setAvgLatencyNanos(final int venueId, final int value) {
        checkVenue(venueId);
        avgLatencyNanos[venueId] = Math.max(0, value);
    }

    public int avgLatencyNanos(final int venueId) {
        checkVenue(venueId);
        return avgLatencyNanos[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= avgLatencyNanos.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
