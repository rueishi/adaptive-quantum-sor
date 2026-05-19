package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic venue session and outage simulation.
 *
 * <p>Role in system: this is the direct unit-test surface for P5-TC-003 venue
 * availability behavior.</p>
 *
 * <p>Relationships: exercises {@link VenueSessionSimulator} writes into
 * {@link VenueSessionState} using profile data from {@link ScenarioState}.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit with simulator tests.</p>
 *
 * <p>Design intent: make unavailable venues explicit and deterministic so
 * execution tests can prove they are never treated as executable.</p>
 */
final class VenueSessionSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void openAllMarksAllVenuesOpen() {
        final VenueSessionState state = new VenueSessionState(CONFIG.venueCount());

        new VenueSessionSimulator(CONFIG).openAll(state);

        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            assertEquals(VenueStatus.OPEN, state.status(venueId));
            assertTrue(state.isAvailable(venueId));
        }
    }

    @Test
    void applyOutagePatternIsDeterministic() {
        final VenueSessionState first = new VenueSessionState(CONFIG.venueCount());
        final VenueSessionState second = new VenueSessionState(CONFIG.venueCount());
        final VenueSessionSimulator simulator = new VenueSessionSimulator(CONFIG);

        simulator.applyOutagePattern(first);
        simulator.applyOutagePattern(second);

        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            assertEquals(first.status(venueId), second.status(venueId));
            assertEquals(venueId == 4 ? VenueStatus.OUTAGE : VenueStatus.OPEN, first.status(venueId));
        }
    }

    @Test
    void applyScenarioProducesDeterministicOutageAndRecoveryWindows() {
        final VenueSessionState state = new VenueSessionState(CONFIG.venueCount());
        final ScenarioState scenario = ScenarioState.fresh(new ScenarioSpec("venue-outage", 1L, 4, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 3, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final VenueSessionSimulator simulator = new VenueSessionSimulator(CONFIG);

        simulator.applyScenario(state, scenario);
        assertEquals(VenueStatus.OUTAGE, state.status(4));
        scenario.clock().advance();
        scenario.clock().advance();
        simulator.applyScenario(state, scenario);
        assertEquals(VenueStatus.OPEN, state.status(4));
    }

    @Test
    void invalidInputsAreRejected() {
        final VenueSessionSimulator simulator = new VenueSessionSimulator(CONFIG);
        final VenueSessionState state = new VenueSessionState(CONFIG.venueCount());

        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new VenueSessionSimulator(null)
        ).getMessage());
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.openAll(null)
        ).getMessage());
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.applyOutagePattern(null)
        ).getMessage());
        assertEquals("state and scenarioState must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.applyScenario(state, null)
        ).getMessage());
    }
}
