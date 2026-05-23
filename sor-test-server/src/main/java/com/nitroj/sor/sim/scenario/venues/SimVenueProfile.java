package com.nitroj.sor.sim.scenario.venues;

/**
 * Responsibility: deterministic venue behavior categories for simulator runs.
 *
 * <p>Role in system: gives market-data, session, throttle, and venue-outcome
 * simulators one shared profile vocabulary without importing legacy scenario
 * enums.</p>
 *
 * <p>Relationships: selected by dense venue ID or by scenario profile input.</p>
 *
 * <p>Lifecycle: enum constants and parameters are stable to preserve seeded
 * replay behavior.</p>
 *
 * <p>Design intent: match the legacy profile multipliers and probabilities so
 * fixed-seed parity tests can compare old and new behavior.</p>
 */
public enum SimVenueProfile {
    TIGHT_DEEP(1, 8_000L, 0, 0),
    WIDE_SLOW(3, 4_000L, 0, 0),
    TOXIC(2, 3_000L, 0, 0),
    STALE_FEED(2, 2_500L, 1_000, 0),
    OUTAGE_PRONE(4, 1_500L, 0, 1_000);

    public final int spreadMultiplier;
    public final long targetQty;
    public final int staleProbabilityBps;
    public final int outageProbabilityBps;

    SimVenueProfile(
            final int spreadMultiplier,
            final long targetQty,
            final int staleProbabilityBps,
            final int outageProbabilityBps
    ) {
        this.spreadMultiplier = spreadMultiplier;
        this.targetQty = targetQty;
        this.staleProbabilityBps = staleProbabilityBps;
        this.outageProbabilityBps = outageProbabilityBps;
    }

    /** Assigns the same stable profile used by legacy scenario simulation. */
    public static SimVenueProfile forVenue(final int venueId) {
        if (venueId < 0) {
            throw new IllegalArgumentException("venueId must be non-negative");
        }
        final SimVenueProfile[] values = values();
        return values[venueId % values.length];
    }
}
