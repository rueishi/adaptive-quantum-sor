package com.nitroj.sor.sim.scenario.venues;

/** Public simulator snapshot of venue throttle limits by venue. */
public final class SimVenueThrottleSnapshot {
    private final int[] maxOrderRatePerSecond;

    public SimVenueThrottleSnapshot(final int venueCount) {
        this.maxOrderRatePerSecond = new int[venueCount];
    }

    public void setMaxOrderRatePerSecond(final int venueId, final int rate) {
        maxOrderRatePerSecond[venueId] = Math.max(0, rate);
    }

    public int maxOrderRatePerSecond(final int venueId) {
        return maxOrderRatePerSecond[venueId];
    }
}
