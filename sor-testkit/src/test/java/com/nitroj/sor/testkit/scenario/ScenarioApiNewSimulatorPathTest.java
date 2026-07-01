package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.core.config.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-28 API-facing scenario summaries stay deterministic. */
final class ScenarioApiNewSimulatorPathTest {
    @Test
    void repeatedApiStyleRunIsReplayEquivalent() {
        final SorConfig config = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);
        final ScenarioSpec spec = ScenarioSpec.defaultSpec(config, 99L);

        final ScenarioSummary first = new ScenarioRunner().run(spec);
        final ScenarioSummary second = new ScenarioRunner().run(spec);

        assertEquals(first, second);
    }
}
