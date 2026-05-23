package com.nitroj.adaptive.quantum.sor.stats;

/**
 * Responsibility: store queue survival estimates per venue.
 *
 * <p>Role in system: Phase 1 has no full L3 matching engine, but policy and ML
 * layers still need a bounded placeholder for queue behavior.</p>
 *
 * <p>Relationships: future L3 simulation can update this class directly while
 * tactical policy output consumes the same bps scale.</p>
 *
 * <p>Lifecycle: neutral at construction and updated by feature or L3 simulators.</p>
 *
 * <p>Design intent: a small bps array gives deterministic behavior without
 * expanding scope into exchange fidelity.</p>
 */
public final class QueueStats {
    private final int[] queueSurvivalBps;

    public QueueStats(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.queueSurvivalBps = new int[venueCount];
        for (int i = 0; i < venueCount; i++) {
            queueSurvivalBps[i] = 5_000;
        }
    }

    /** Sets bounded queue survival probability. */
    public void setQueueSurvivalBps(final int venueId, final int value) {
        checkVenue(venueId);
        queueSurvivalBps[venueId] = ExecutionOutcomeStore.clampBps(value);
    }

    public int queueSurvivalBps(final int venueId) {
        checkVenue(venueId);
        return queueSurvivalBps[venueId];
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= queueSurvivalBps.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
