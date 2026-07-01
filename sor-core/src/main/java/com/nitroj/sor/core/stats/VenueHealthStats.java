package com.nitroj.sor.core.stats;

/**
 * Responsibility: store venue health, rejects, and malformed-event counters.
 *
 * <p>Role in system: feature aggregation updates this near-hot-path guard
 * signal so optimizers and future execution guards can avoid unhealthy venues.</p>
 *
 * <p>Relationships: reads simulated reject/outage/throttle behavior and is
 * consumed by policy compilation and ML stubs.</p>
 *
 * <p>Lifecycle: initialized to healthy defaults, then refreshed on aggregation.</p>
 *
 * <p>Design intent: malformed outcome references are counted here so bad data
 * can be ignored without poisoning stats.</p>
 */
public final class VenueHealthStats {
    private final int[] rejectRateBps;
    private final int[] healthBps;
    private int badEventCount;

    public VenueHealthStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.rejectRateBps = new int[venueCount];
        this.healthBps = new int[venueCount];
        for (int i = 0; i < venueCount; i++) {
            healthBps[i] = 10_000;
        }
    }

    /** Updates bounded reject rate and derived health for a venue. */
    public void update(final int venueId, final int rejectRateBps) {
        checkVenue(venueId);
        final int boundedReject = ExecutionOutcomeStore.clampBps(rejectRateBps);
        this.rejectRateBps[venueId] = boundedReject;
        this.healthBps[venueId] = 10_000 - boundedReject;
    }

    /** Counts an ignored malformed outcome reference. */
    public void recordBadEvent() {
        badEventCount++;
    }

    public int rejectRateBps(final int venueId) {
        checkVenue(venueId);
        return rejectRateBps[venueId];
    }

    public int healthBps(final int venueId) {
        checkVenue(venueId);
        return healthBps[venueId];
    }

    public int badEventCount() {
        return badEventCount;
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= rejectRateBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
