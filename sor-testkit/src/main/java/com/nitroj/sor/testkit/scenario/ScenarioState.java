package com.nitroj.sor.testkit.scenario;

/**
 * Responsibility: hold metadata and deterministic support objects for one
 * scenario run.
 *
 * <p>Role in system: Phase 5 later expands this state boundary with market,
 * session, order, and optimizer state containers. P5-TC-001 establishes the
 * immutable scenario support surface.</p>
 *
 * <p>Relationships: created from {@link ScenarioSpec}; owns
 * {@link ScenarioClock}, {@link ScenarioRandoms}, and profile assignment.</p>
 *
 * <p>Lifecycle: created fresh per scenario run and discarded after the run.</p>
 *
 * <p>Design intent: gives scenario tests a clean isolated boundary without
 * mutating live application state.</p>
 */
public final class ScenarioState {
    private final ScenarioSpec spec;
    private final ScenarioClock clock;
    private final ScenarioRandoms randoms;
    private final ScenarioVenueProfile[] venueProfiles;

    private ScenarioState(final ScenarioSpec spec) {
        this.spec = spec;
        this.clock = new ScenarioClock(0L, 1_000_000L);
        this.randoms = new ScenarioRandoms(spec.seed());
        this.venueProfiles = new ScenarioVenueProfile[spec.config().venueCount()];
        for (int venueId = 0; venueId < venueProfiles.length; venueId++) {
            venueProfiles[venueId] = spec.profileForVenue(venueId);
        }
    }

    public static ScenarioState fresh(final ScenarioSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        return new ScenarioState(spec);
    }

    public ScenarioSpec spec() {
        return spec;
    }

    public ScenarioClock clock() {
        return clock;
    }

    public ScenarioRandoms randoms() {
        return randoms;
    }

    public ScenarioVenueProfile profile(final int venueId) {
        if (venueId < 0 || venueId >= venueProfiles.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
        return venueProfiles[venueId];
    }
}
