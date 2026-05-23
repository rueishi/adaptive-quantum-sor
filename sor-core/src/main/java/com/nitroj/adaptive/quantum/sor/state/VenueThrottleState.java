package com.nitroj.adaptive.quantum.sor.state;

/**
 * Responsibility: store simulated venue throttle limits.
 *
 * <p>Role in system: execution guards and optimizers can use this near-hot-path
 * state to avoid venues that would exceed configured order rates.</p>
 *
 * <p>Relationships: venue throttle simulators update limits; future execution
 * code reads the per-venue maximum.</p>
 *
 * <p>Lifecycle: created with dense venue capacity and updated in place.</p>
 *
 * <p>Design intent: storing only max order rate is enough for P1-TC-004 while
 * leaving room for richer throttle accounting later.</p>
 */
public final class VenueThrottleState {
    private final int[] maxOrderRatePerSecond;

    public VenueThrottleState(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.maxOrderRatePerSecond = new int[venueCount];
    }

    public void setMaxOrderRatePerSecond(final int venueId, final int rate) {
        checkVenue(venueId);
        if (rate < 0) {
            throw new IllegalArgumentException("rate must be non-negative");
        }
        maxOrderRatePerSecond[venueId] = rate;
    }

    public int maxOrderRatePerSecond(final int venueId) {
        checkVenue(venueId);
        return maxOrderRatePerSecond[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= maxOrderRatePerSecond.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
