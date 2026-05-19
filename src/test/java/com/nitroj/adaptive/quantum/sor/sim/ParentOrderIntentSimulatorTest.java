package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic parent-order flow simulation.
 *
 * <p>Role in system: this is the direct unit-test surface for P5-TC-003 order
 * arrival and regime-conditioned parent intent behavior.</p>
 *
 * <p>Relationships: exercises {@link ParentOrderIntentSimulator},
 * {@link ScenarioState}, and {@link ParentOrderIntentQueue} together.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit with simulator tests.</p>
 *
 * <p>Design intent: prove existing deterministic order generation remains
 * stable while scenario-driven APIs use simulated clock time.</p>
 */
final class ParentOrderIntentSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 4, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void sameSeedProducesSameOrderSequence() {
        final ParentOrderIntentSimulator first = new ParentOrderIntentSimulator(CONFIG, 9L);
        final ParentOrderIntentSimulator second = new ParentOrderIntentSimulator(CONFIG, 9L);

        for (int i = 0; i < 8; i++) {
            assertOrderEquals(first.next(1_000L + i), second.next(1_000L + i));
        }
    }

    @Test
    void scenarioNextUsesSimulatedClockAndRegimeDistribution() {
        final ScenarioState volatileScenario = ScenarioState.fresh(spec("volatile-order", RegimeState.VOLATILE, 3));
        final ScenarioState thinScenario = ScenarioState.fresh(spec("thin-order", RegimeState.THIN_BOOK, 3));
        final ParentOrderIntentSimulator volatileSim = new ParentOrderIntentSimulator(CONFIG, 11L);
        final ParentOrderIntentSimulator thinSim = new ParentOrderIntentSimulator(CONFIG, 11L);

        final OrderIntent volatileOrder = volatileSim.next(volatileScenario);
        final OrderIntent thinOrder = thinSim.next(thinScenario);

        assertEquals(0L, volatileOrder.createdAtNanos);
        assertEquals(0L, thinOrder.createdAtNanos);
        assertTrue(volatileOrder.quantity >= 5_000L);
        assertTrue(thinOrder.quantity <= 2_000L);
        assertEquals(CONFIG.urgencyCount() - 1, volatileOrder.urgencyId);
        assertEquals(0, thinOrder.urgencyId);
    }

    @Test
    void offerForCurrentTickChangesCadenceByRegimeAndHonorsCapacity() {
        final ParentOrderIntentSimulator simulator = new ParentOrderIntentSimulator(CONFIG, 15L);
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(3);
        final ScenarioState volatileScenario = ScenarioState.fresh(spec("volatile-cadence", RegimeState.VOLATILE, 1));
        final ScenarioState thinScenario = ScenarioState.fresh(spec("thin-cadence", RegimeState.THIN_BOOK, 2));

        assertEquals(2, simulator.offerForCurrentTick(volatileScenario, queue));
        assertEquals(2, queue.size());
        assertEquals(1, simulator.offerForCurrentTick(thinScenario, queue));
        thinScenario.clock().advance();
        assertEquals(0, simulator.offerForCurrentTick(thinScenario, queue));
        assertEquals(3, queue.size());
    }

    @Test
    void invalidInputsAreRejected() {
        final ParentOrderIntentSimulator simulator = new ParentOrderIntentSimulator(CONFIG, 21L);
        final ScenarioState scenario = ScenarioState.fresh(spec("normal", RegimeState.NORMAL, 1));

        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new ParentOrderIntentSimulator(null, 1L)
        ).getMessage());
        assertEquals("scenarioState must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.next(null)
        ).getMessage());
        assertEquals("scenarioState and queue must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.offerForCurrentTick(scenario, null)
        ).getMessage());
    }

    private static ScenarioSpec spec(final String id, final int regimeId, final int ticks) {
        return new ScenarioSpec(id, 1L, ticks, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, ticks - 1, regimeId)},
                true,
                ScenarioSimulatorConfig.defaults());
    }

    private static void assertOrderEquals(final OrderIntent first, final OrderIntent second) {
        assertEquals(first.parentOrderId, second.parentOrderId);
        assertEquals(first.instrumentId, second.instrumentId);
        assertEquals(first.side, second.side);
        assertEquals(first.quantity, second.quantity);
        assertEquals(first.urgencyId, second.urgencyId);
        assertEquals(first.createdAtNanos, second.createdAtNanos);
    }
}
