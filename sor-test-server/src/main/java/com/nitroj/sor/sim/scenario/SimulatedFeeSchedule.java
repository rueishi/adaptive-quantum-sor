package com.nitroj.sor.sim.scenario;

/**
 * Responsibility: generate deterministic maker/taker fee snapshots.
 *
 * <p>Role in system: supplies simulator scenarios with the same dense
 * instrument/venue fee layout previously produced by the legacy simulator.</p>
 *
 * <p>Relationships: returns simulator-local {@link SimFeeScheduleSnapshot}
 * instances so production code in this module does not import core metadata
 * classes.</p>
 *
 * <p>Lifecycle: created at scenario setup and invoked whenever a fresh immutable
 * fee view is needed.</p>
 *
 * <p>Design intent: preserve the simple venue-indexed fee formulas used by the
 * legacy tests while presenting an integration-friendly component.</p>
 */
public final class SimulatedFeeSchedule {
    /** Generates the legacy-equivalent maker/taker fee matrix. */
    public SimFeeScheduleSnapshot generate(final SimConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final SimFeeScheduleSnapshot snapshot = new SimFeeScheduleSnapshot(config);
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                snapshot.setFees(instrumentId, venueId, venueId % 3, 1L + venueId % 5);
            }
        }
        return snapshot;
    }
}
