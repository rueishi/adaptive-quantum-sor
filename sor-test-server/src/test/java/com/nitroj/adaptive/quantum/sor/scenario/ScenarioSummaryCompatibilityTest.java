package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies P8-28 existing ScenarioSummary fields remain populated. */
final class ScenarioSummaryCompatibilityTest {
    @Test
    void summaryKeepsLegacyCompatibleCounters() {
        final ScenarioSummary summary = new ScenarioRunner().run(new ScenarioSpec("compat", 101L, 3,
                new SorConfig(1, 2, 3, 1, SorConfig.RuntimeMode.DEMO, true),
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 0), new ScenarioWindow(1, 1, 1),
                        new ScenarioWindow(2, 2, 2)},
                true,
                ScenarioSimulatorConfig.defaults()));

        assertEquals(1, summary.detectedNormalCount());
        assertEquals(1, summary.detectedVolatileCount());
        assertEquals(1, summary.detectedThinBookCount());
        assertTrue(summary.bookChecksum() != 0L);
    }
}
