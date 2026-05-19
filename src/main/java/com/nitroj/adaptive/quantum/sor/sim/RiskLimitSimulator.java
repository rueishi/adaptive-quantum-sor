package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;

/**
 * Responsibility: generate deterministic pre-trade risk limits.
 *
 * <p>Role in system: supplies policy validation and execution guards with
 * bounded child size, notional, and participation limits.</p>
 *
 * <p>Relationships: writes {@link RiskLimitSnapshot} using configured dense
 * instrument and venue dimensions.</p>
 *
 * <p>Lifecycle: created during scenario setup and treated as a snapshot until
 * replaced by another scenario.</p>
 *
 * <p>Design intent: positive limits and bps caps are sufficient for Phase 1
 * without modeling a full risk service.</p>
 */
public final class RiskLimitSimulator {
    private final SorConfig config;

    public RiskLimitSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /** Creates deterministic bounded risk limits. */
    public RiskLimitSnapshot generate() {
        final RiskLimitSnapshot snapshot = new RiskLimitSnapshot(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            snapshot.setMaxChildQty(instrumentId, 10_000L + instrumentId * 100L);
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                snapshot.setVenueLimits(instrumentId, venueId, 1_000_000L + venueId * 10_000L, 2_500);
            }
        }
        return snapshot;
    }
}
