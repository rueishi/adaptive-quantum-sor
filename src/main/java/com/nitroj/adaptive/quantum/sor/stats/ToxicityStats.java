package com.nitroj.adaptive.quantum.sor.stats;

/**
 * Responsibility: store adverse-selection/toxicity estimates per venue.
 *
 * <p>Role in system: feature aggregation and the ML stub use this as a
 * bounded penalty signal for venue quality.</p>
 *
 * <p>Relationships: mirrored by {@link VenueStatsState} for consolidated
 * optimizer input.</p>
 *
 * <p>Lifecycle: neutral at construction and updated from simulated post-fill
 * price drift.</p>
 *
 * <p>Design intent: bps clamping prevents invalid model inputs.</p>
 */
public final class ToxicityStats {
    private final int[] toxicityBps;

    public ToxicityStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.toxicityBps = new int[venueCount];
    }

    /** Sets the bounded toxicity value for a venue. */
    public void setToxicityBps(final int venueId, final int value) {
        checkVenue(venueId);
        toxicityBps[venueId] = ExecutionOutcomeStore.clampBps(value);
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
}
