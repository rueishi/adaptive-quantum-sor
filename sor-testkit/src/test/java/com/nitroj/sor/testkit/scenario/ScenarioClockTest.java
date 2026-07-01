package com.nitroj.sor.testkit.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic simulated scenario time.
 *
 * <p>Role in system: proves scenario-driven tests can use timestamps without
 * depending on wall-clock APIs.</p>
 *
 * <p>Relationships: tests {@link ScenarioClock}, which is consumed by
 * {@link ScenarioRunner} and later order/session simulators.</p>
 *
 * <p>Lifecycle: executed as P5-TC-001 unit coverage.</p>
 *
 * <p>Design intent: make simulated time a pure function of start nanos and tick
 * count.</p>
 */
final class ScenarioClockTest {
    @Test
    void scenarioClockAdvancesSimulatedNanosDeterministically() {
        final ScenarioClock clock = new ScenarioClock(10L, 5L);

        assertEquals(0, clock.tick());
        assertEquals(10L, clock.nowNanos());
        clock.advance();
        clock.advance();

        assertEquals(2, clock.tick());
        assertEquals(20L, clock.nowNanos());
    }

    @Test
    void scenarioClockRejectsInvalidConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioClock(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioClock(0L, 0L));
    }
}
