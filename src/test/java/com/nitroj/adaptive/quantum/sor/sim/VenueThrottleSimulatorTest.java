package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic venue throttle simulation.
 *
 * <p>Role in system: provides direct unit coverage for P5-TC-003 throttle state
 * refreshes.</p>
 *
 * <p>Relationships: exercises {@link VenueThrottleSimulator} writes into
 * {@link VenueThrottleState} from baseline and scenario-aware APIs.</p>
 *
 * <p>Lifecycle: run by Gradle/JUnit with simulator tests.</p>
 *
 * <p>Design intent: preserve legacy throttle defaults while proving scenario
 * regimes can constrain order rates deterministically.</p>
 */
final class VenueThrottleSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void populatePreservesBaselineRates() {
        final VenueThrottleState state = new VenueThrottleState(CONFIG.venueCount());

        new VenueThrottleSimulator(CONFIG).populate(state);

        assertEquals(50, state.maxOrderRatePerSecond(0));
        assertEquals(70, state.maxOrderRatePerSecond(4));
    }

    @Test
    void scenarioPopulateReducesRatesInVolatileAndThinWindows() {
        final VenueThrottleSimulator simulator = new VenueThrottleSimulator(CONFIG);
        final VenueThrottleState volatileState = new VenueThrottleState(CONFIG.venueCount());
        final VenueThrottleState thinState = new VenueThrottleState(CONFIG.venueCount());

        simulator.populate(volatileState, ScenarioState.fresh(spec("volatile", RegimeState.VOLATILE)));
        simulator.populate(thinState, ScenarioState.fresh(spec("thin", RegimeState.THIN_BOOK)));

        assertEquals(25, volatileState.maxOrderRatePerSecond(0));
        assertEquals(16, thinState.maxOrderRatePerSecond(0));
        assertEquals(20, volatileState.maxOrderRatePerSecond(4));
        assertEquals(20, thinState.maxOrderRatePerSecond(4));
    }

    @Test
    void invalidInputsAreRejected() {
        final VenueThrottleSimulator simulator = new VenueThrottleSimulator(CONFIG);
        final VenueThrottleState state = new VenueThrottleState(CONFIG.venueCount());

        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new VenueThrottleSimulator(null)
        ).getMessage());
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.populate(null)
        ).getMessage());
        assertEquals("state and scenarioState must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.populate(state, null)
        ).getMessage());
    }

    private static ScenarioSpec spec(final String id, final int regimeId) {
        return new ScenarioSpec(id, 1L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, regimeId)},
                true,
                ScenarioSimulatorConfig.defaults());
    }
}
