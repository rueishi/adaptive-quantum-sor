package com.nitroj.adaptive.quantum.sor.state;

/**
 * Responsibility: track simulated market data feed health per venue.
 *
 * <p>Role in system: routing and feature aggregation can reject stale or failed
 * venue data before trusting book quantities.</p>
 *
 * <p>Relationships: market data simulators update this state, while execution
 * and optimizer inputs read it as a guard.</p>
 *
 * <p>Lifecycle: allocated once for dense venues and updated with feed events.</p>
 *
 * <p>Design intent: explicit staleness flags make failure cases testable without
 * real feed connectivity.</p>
 */
public final class FeedHealthState {
    private final boolean[] staleByVenue;
    private final long[] lastSequenceByVenue;

    public FeedHealthState(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.staleByVenue = new boolean[venueCount];
        this.lastSequenceByVenue = new long[venueCount];
    }

    public void update(final int venueId, final long sequence, final boolean stale) {
        checkVenue(venueId);
        if (sequence < lastSequenceByVenue[venueId]) {
            throw new IllegalArgumentException("sequence must not go backwards");
        }
        lastSequenceByVenue[venueId] = sequence;
        staleByVenue[venueId] = stale;
    }

    public boolean isStale(final int venueId) {
        checkVenue(venueId);
        return staleByVenue[venueId];
    }

    public long lastSequence(final int venueId) {
        checkVenue(venueId);
        return lastSequenceByVenue[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= staleByVenue.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
