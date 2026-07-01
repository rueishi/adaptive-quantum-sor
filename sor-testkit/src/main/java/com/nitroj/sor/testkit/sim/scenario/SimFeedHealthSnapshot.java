package com.nitroj.sor.testkit.sim.scenario;

/** Feed-health state for stale-feed scenario windows. */
public final class SimFeedHealthSnapshot {
    private final long[] lastTickByVenue;
    private final boolean[] staleByVenue;

    public SimFeedHealthSnapshot(final int venueCount) {
        this.lastTickByVenue = new long[venueCount];
        this.staleByVenue = new boolean[venueCount];
    }

    public void update(final int venueId, final long tick, final boolean stale) {
        lastTickByVenue[venueId] = tick;
        staleByVenue[venueId] = stale;
    }

    public long lastTick(final int venueId) {
        return lastTickByVenue[venueId];
    }

    public boolean isStale(final int venueId) {
        return staleByVenue[venueId];
    }
}
