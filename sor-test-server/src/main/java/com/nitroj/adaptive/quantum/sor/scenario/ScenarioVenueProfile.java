package com.nitroj.adaptive.quantum.sor.scenario;

/**
 * Responsibility: define deterministic venue behavior categories for scenario
 * simulation.
 *
 * <p>Role in system: scenario contracts assign each venue a stable profile so
 * market-data, session, and venue-outcome simulators can share the same
 * explanation for spread, depth, latency, toxicity, and outage tendencies.</p>
 *
 * <p>Relationships: used by {@link ScenarioSpec} and {@link ScenarioState};
 * later simulator task cards consume profile parameters.</p>
 *
 * <p>Lifecycle: enum constants are stable for the Phase 5 scenario surface.</p>
 *
 * <p>Design intent: deterministic assignment by venue ID keeps scenario tests
 * reproducible and avoids config churn while preserving realistic venue
 * diversity.</p>
 */
public enum ScenarioVenueProfile {
    TIGHT_DEEP(1, 8_000L, 0, 0),
    WIDE_SLOW(3, 4_000L, 0, 0),
    TOXIC(2, 3_000L, 0, 0),
    STALE_FEED(2, 2_500L, 1_000, 0),
    OUTAGE_PRONE(4, 1_500L, 0, 1_000);

    public final int spreadMultiplier;
    public final long targetQty;
    public final int staleProbabilityBps;
    public final int outageProbabilityBps;

    ScenarioVenueProfile(
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

    /**
     * Assigns a stable profile by dense venue ID.
     */
    public static ScenarioVenueProfile forVenue(final int venueId) {
        if (venueId < 0) {
            throw new IllegalArgumentException("venueId must be non-negative");
        }
        final ScenarioVenueProfile[] profiles = values();
        return profiles[venueId % profiles.length];
    }
}
