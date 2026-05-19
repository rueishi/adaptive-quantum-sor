package com.nitroj.adaptive.quantum.sor.state;

/**
 * Responsibility: store deterministic simulated behavior profile values per venue.
 *
 * <p>Role in system: venue simulation uses these values to represent fast,
 * slow, toxic, reject-prone, or fading-liquidity venues without external market
 * connectivity.</p>
 *
 * <p>Relationships: feature aggregation and optimizer stubs can later derive
 * stats from these profiles.</p>
 *
 * <p>Lifecycle: initialized by venue behavior simulators and updated as
 * scenarios change.</p>
 *
 * <p>Design intent: bps fields keep behavior profiles bounded and easy to test.</p>
 */
public final class VenueBehaviorState {
    private final int[] toxicityBps;
    private final int[] rejectRateBps;
    private final int[] fillProbabilityBps;

    public VenueBehaviorState(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.toxicityBps = new int[venueCount];
        this.rejectRateBps = new int[venueCount];
        this.fillProbabilityBps = new int[venueCount];
    }

    /**
     * Updates bounded behavior profile values for a venue.
     */
    public void update(final int venueId, final int toxicityBps, final int rejectRateBps, final int fillProbabilityBps) {
        checkVenue(venueId);
        this.toxicityBps[venueId] = checkBps("toxicityBps", toxicityBps);
        this.rejectRateBps[venueId] = checkBps("rejectRateBps", rejectRateBps);
        this.fillProbabilityBps[venueId] = checkBps("fillProbabilityBps", fillProbabilityBps);
    }

    public int toxicityBps(final int venueId) {
        checkVenue(venueId);
        return toxicityBps[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= toxicityBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }

    private static int checkBps(final String name, final int value) {
        if (value < 0 || value > 10_000) {
            throw new IllegalArgumentException(name + " must be in [0,10000]");
        }
        return value;
    }
}
