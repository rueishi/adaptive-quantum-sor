package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.core.config.SorConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies `ScenarioRunner` uses the current simulator-backed
 * engine path.
 *
 * <p>Role in system: protects the Phase 9 real-path scenario requirement by
 * asserting scenario summaries still work and the runner no longer uses the
 * simulator-local market book as primary SOR evidence.</p>
 *
 * <p>Relationships: exercises {@link ScenarioRunner}, {@link ScenarioSpec},
 * and source-level ownership guards for the runner implementation.</p>
 *
 * <p>Lifecycle: JUnit test run by `:sor-test-server:test`.</p>
 *
 * <p>Design intent: keep deterministic scenario replay while ensuring market
 * data and orders flow through the real engine path.</p>
 */
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

    @Test
    void runnerDoesNotUseSimulatorBookAsPrimaryRoutingEvidence() throws IOException {
        final String source = Files.readString(Path.of(
                "sor-testkit/src/main/java/com/nitroj/sor/testkit/scenario/ScenarioRunner.java"));

        assertTrue(!source.contains("marketData.currentBook()"),
                "ScenarioRunner must route through SorEngine instead of simulator-local currentBook()");
    }
}
