package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioVenueProfile;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

/**
 * Responsibility: generate deterministic venue throttle limits.
 *
 * <p>Role in system: provides simulated order-rate guard data before any real
 * venue connectivity exists.</p>
 *
 * <p>Relationships: writes {@link VenueThrottleState} for future policy and
 * execution guards.</p>
 *
 * <p>Lifecycle: run during scenario setup and refreshed when throttle scenarios
 * change.</p>
 *
 * <p>Design intent: bounded non-negative rates keep downstream validation simple.</p>
 */
public final class VenueThrottleSimulator {
    private final SorConfig config;

    public VenueThrottleSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /** Populates per-venue max order rates. */
    public void populate(final VenueThrottleState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            state.setMaxOrderRatePerSecond(venueId, 50 + venueId * 5);
        }
    }

    /**
     * Populates scenario-aware venue throttle limits for the current tick.
     *
     * <p>Core logic: the legacy baseline rate remains the starting point, then
     * volatile markets and outage-prone venues receive lower deterministic
     * rates. Rates are never negative and no executable state is inferred from
     * throttles; venue availability remains owned by {@link VenueSessionSimulator}.</p>
     *
     * @param state throttle state to update
     * @param scenarioState current scenario context and simulated clock
     */
    public void populate(final VenueThrottleState state, final ScenarioState scenarioState) {
        if (state == null || scenarioState == null) {
            throw new IllegalArgumentException("state and scenarioState must not be null");
        }
        final int regimeId = scenarioState.spec().regimeAt(scenarioState.clock().tick());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            int rate = 50 + venueId * 5;
            if (regimeId == RegimeState.VOLATILE) {
                rate = rate / 2;
            } else if (regimeId == RegimeState.THIN_BOOK) {
                rate = rate / 3;
            }
            if (scenarioState.profile(venueId) == ScenarioVenueProfile.OUTAGE_PRONE) {
                rate = Math.min(rate, 20);
            }
            state.setMaxOrderRatePerSecond(venueId, Math.max(0, rate));
        }
    }
}
