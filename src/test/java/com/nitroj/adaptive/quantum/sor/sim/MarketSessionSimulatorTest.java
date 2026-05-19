package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic market-session simulation.
 *
 * <p>Role in system: this is the direct unit-test surface for P5-TC-003 market
 * halt/auction state, separate from scenario orchestration tests.</p>
 *
 * <p>Relationships: exercises {@link MarketSessionSimulator} writes into
 * {@link MarketSessionState} using optional {@link ScenarioState} input.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit whenever session simulator behavior
 * changes.</p>
 *
 * <p>Design intent: preserve legacy all-open behavior while proving scenario
 * windows can make an instrument non-executable deterministically.</p>
 */
final class MarketSessionSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(3, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void openAllMarksAllInstrumentsOpen() {
        final MarketSessionState state = new MarketSessionState(CONFIG.instrumentCount());

        new MarketSessionSimulator(CONFIG).openAll(state);

        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(state.isOpen(instrumentId));
        }
    }

    @Test
    void applyScenarioClosesRotatingInstrumentInThinWindow() {
        final ScenarioState scenario = ScenarioState.fresh(thinSpec());
        final MarketSessionState state = new MarketSessionState(CONFIG.instrumentCount());
        final MarketSessionSimulator simulator = new MarketSessionSimulator(CONFIG);

        simulator.applyScenario(state, scenario);

        assertFalse(state.isOpen(0));
        assertTrue(state.isOpen(1));
        assertTrue(state.isOpen(2));
        scenario.clock().advance();
        simulator.applyScenario(state, scenario);
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(state.isOpen(instrumentId));
        }
    }

    @Test
    void invalidInputsAreRejected() {
        final MarketSessionSimulator simulator = new MarketSessionSimulator(CONFIG);
        final MarketSessionState state = new MarketSessionState(CONFIG.instrumentCount());

        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new MarketSessionSimulator(null)
        ).getMessage());
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.openAll(null)
        ).getMessage());
        assertEquals("state and scenarioState must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.applyScenario(state, null)
        ).getMessage());
    }

    private static ScenarioSpec thinSpec() {
        return new ScenarioSpec("thin-session", 1L, 2, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, RegimeState.THIN_BOOK)},
                true,
                ScenarioSimulatorConfig.defaults());
    }
}
