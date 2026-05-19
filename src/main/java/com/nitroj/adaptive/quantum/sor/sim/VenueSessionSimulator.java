package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioVenueProfile;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

/**
 * Responsibility: populate deterministic venue open/outage session state.
 *
 * <p>Role in system: simulates exchange availability for policy and execution
 * guards.</p>
 *
 * <p>Relationships: writes {@link VenueSessionState} using {@link VenueStatus}
 * constants.</p>
 *
 * <p>Lifecycle: called during scenario setup or when a scenario toggles outages.</p>
 *
 * <p>Design intent: every fifth venue can be marked outage to exercise edge
 * behavior while default generation leaves venues open.</p>
 */
public final class VenueSessionSimulator {
    private final SorConfig config;

    public VenueSessionSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /** Marks all venues open. */
    public void openAll(final VenueSessionState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            state.setStatus(venueId, VenueStatus.OPEN);
        }
    }

    /** Applies a deterministic outage pattern for edge-case simulations. */
    public void applyOutagePattern(final VenueSessionState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            state.setStatus(venueId, venueId % 5 == 4 ? VenueStatus.OUTAGE : VenueStatus.OPEN);
        }
    }

    /**
     * Applies deterministic venue outage/recovery windows for a scenario tick.
     *
     * <p>Core logic: outage-prone profile venues enter outage on a stable
     * modulo schedule, with longer outage windows during volatile regimes. All
     * other profiles remain open in this task card. The method writes only the
     * supplied state so Gradle scenario tests and live notebook tests can reset
     * state explicitly.</p>
     *
     * @param state venue-session state to mutate
     * @param scenarioState current scenario context and simulated clock
     */
    public void applyScenario(final VenueSessionState state, final ScenarioState scenarioState) {
        if (state == null || scenarioState == null) {
            throw new IllegalArgumentException("state and scenarioState must not be null");
        }
        final int tick = scenarioState.clock().tick();
        final int regimeId = scenarioState.spec().regimeAt(tick);
        final int outageCycle = regimeId == RegimeState.VOLATILE ? 6 : 10;
        final int outageLength = regimeId == RegimeState.VOLATILE ? 3 : 2;
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final ScenarioVenueProfile profile = scenarioState.profile(venueId);
            final boolean outage = profile == ScenarioVenueProfile.OUTAGE_PRONE
                    && Math.floorMod(tick, outageCycle) < outageLength;
            state.setStatus(venueId, outage ? VenueStatus.OUTAGE : VenueStatus.OPEN);
        }
    }
}
