package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;

/**
 * Responsibility: generate deterministic maker/taker fee schedules.
 *
 * <p>Role in system: replaces external fee files for Phase 1 policy and static
 * routing tests.</p>
 *
 * <p>Relationships: writes {@link FeeScheduleSnapshot} in instrument/venue
 * layout.</p>
 *
 * <p>Lifecycle: called at scenario setup to produce a fresh snapshot.</p>
 *
 * <p>Design intent: small integer tick penalties are deterministic and avoid
 * floating point behavior.</p>
 */
public final class FeeScheduleSimulator {
    private final SorConfig config;

    public FeeScheduleSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /** Creates a bounded deterministic fee snapshot. */
    public FeeScheduleSnapshot generate() {
        final FeeScheduleSnapshot snapshot = new FeeScheduleSnapshot(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                snapshot.setFees(instrumentId, venueId, venueId % 3, 1 + venueId % 5);
            }
        }
        return snapshot;
    }
}
