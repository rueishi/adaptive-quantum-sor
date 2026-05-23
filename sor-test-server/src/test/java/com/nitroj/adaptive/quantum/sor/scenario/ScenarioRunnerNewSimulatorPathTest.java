package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies P8-28 core ScenarioRunner delegates to the new simulator path. */
final class ScenarioRunnerNewSimulatorPathTest {
    @Test
    void runPreservesSummaryShapeThroughNewSimulatorPath() {
        final ScenarioSpec spec = new ScenarioSpec("new-sim-path", 55L, 4,
                new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true),
                new ScenarioWindow[]{new ScenarioWindow(0, 1, 0), new ScenarioWindow(2, 3, 1)},
                true,
                ScenarioSimulatorConfig.defaults());

        final ScenarioSummary summary = new ScenarioRunner().run(spec);

        assertEquals("new-sim-path", summary.scenarioId());
        assertEquals(4, summary.ticksRun());
        assertTrue(summary.orderCount() > 0);
        assertEquals(summary.orderCount(), summary.routeCount());
    }
}
