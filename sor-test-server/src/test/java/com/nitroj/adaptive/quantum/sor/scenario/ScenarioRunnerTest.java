package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.sor.sim.scenario.SimulatorHealthState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify canonical scenario-runner replay behavior.
 *
 * <p>Role in system: this is the direct P5-TC-005 unit-test surface for
 * scenario orchestration and optimizer input lineage gating.</p>
 *
 * <p>Relationships: exercises {@link ScenarioRunner}, {@link ScenarioSpec},
 * {@link ScenarioSummary}, and {@link SimulatorHealthState} together.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit as scenario-unit coverage.</p>
 *
 * <p>Design intent: prove fixed scenario id/config/seed/ticks replay exactly
 * without relying on wall-clock time.</p>
 */
final class ScenarioRunnerTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void runReturnsDeterministicSummaryForFixedSeed() {
        final ScenarioSpec spec = spec(91L);

        final ScenarioSummary first = new ScenarioRunner().run(spec);
        final ScenarioSummary second = new ScenarioRunner().run(spec);

        ScenarioAssertions.assertReplayEquivalent(first, second);
        assertEquals("runner-nvt", first.scenarioId());
        assertEquals(9, first.ticksRun());
        assertTrue(first.orderCount() > 0);
        assertTrue(first.outcomeCount() >= first.childOrderCount());
    }

    @Test
    void runDoesNotUseWallClockTime() {
        final ScenarioSpec spec = spec(97L);

        final ScenarioSummary before = new ScenarioRunner().run(spec);
        final ScenarioSummary after = new ScenarioRunner().run(spec);

        assertEquals(before, after);
        assertEquals(before.bookChecksum(), after.bookChecksum());
    }

    @Test
    void optimizerLineageIsBlockedWhenSimulatorHealthFailed() {
        final ScenarioRunner runner = new ScenarioRunner();
        final ScenarioSpec spec = spec(101L);
        final ScenarioSummary summary = runner.run(spec);
        final SimulatorHealthState failed = new SimulatorHealthState();
        failed.markFailed("MarketDataSimulator", "bad tick", 1L);

        assertNull(runner.optimizerInputLineage(summary, spec, failed));
        assertEquals("summary, spec, and simulatorHealth must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> runner.optimizerInputLineage(summary, spec, null)
        ).getMessage());
    }

    private static ScenarioSpec spec(final long seed) {
        return new ScenarioSpec("runner-nvt", seed, 9, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 2, 0),
                        new ScenarioWindow(3, 5, 1),
                        new ScenarioWindow(6, 8, 2)
                },
                true,
                ScenarioSimulatorConfig.defaults());
    }
}
