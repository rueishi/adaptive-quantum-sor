package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.testkit.ml.FeatureDatasetExporter;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import com.nitroj.sor.testkit.sim.scenario.SimulatorHealthState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify scenario lineage reaches optimizer and dataset
 * metadata surfaces.
 *
 * <p>Role in system: this is the P5-TC-005 integration surface for replay
 * metadata and partial-failure publication gating.</p>
 *
 * <p>Relationships: combines {@link ScenarioRunner},
 * {@link PolicyOptimizationInput}, {@link FeatureDatasetExporter}, and
 * {@link SimulatorHealthState}.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit after scenario unit tests.</p>
 *
 * <p>Design intent: keep lineage compact and auditable without requiring a real
 * optimizer or external dataset registry.</p>
 */
final class ScenarioOptimizerLineageIntegrationTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @TempDir
    Path tempDir;

    @Test
    void optimizerInputSnapshotReferencesScenarioLineage() {
        final ScenarioRunner runner = new ScenarioRunner();
        final ScenarioSpec spec = spec(161L);
        final ScenarioSummary summary = runner.run(spec);
        final PolicyOptimizationInput input = runner.optimizerInputLineage(summary, spec, new SimulatorHealthState());

        assertNotNull(input);
        assertEquals(summary.scenarioId(), input.scenarioId);
        assertEquals(summary.seed(), input.scenarioSeed);
        assertEquals(summary.ticksRun(), input.scenarioTicks);
        assertEquals(0, input.scenarioStartTickInclusive);
        assertEquals(summary.ticksRun() - 1, input.scenarioEndTickInclusive);
        assertTrue(input.scenarioInputPublishable);
        assertEquals(CONFIG.instrumentCount(), input.instrumentCount);
    }

    @Test
    void featureDatasetExportReferencesScenarioLineage() throws Exception {
        final ScenarioSummary summary = new ScenarioRunner().run(spec(163L));
        final Path output = tempDir.resolve("scenario-lineage.properties");

        new FeatureDatasetExporter().exportScenarioLineage(summary, output);

        final String content = Files.readString(output);
        assertTrue(content.contains("scenarioId=lineage-nvt"));
        assertTrue(content.contains("scenarioSeed=163"));
        assertTrue(content.contains("scenarioTicks=6"));
        assertTrue(content.contains("bookChecksum=" + summary.bookChecksum()));
    }

    @Test
    void simulatorFailurePreventsOptimizerPublicationFromPartialState() {
        final ScenarioRunner runner = new ScenarioRunner();
        final ScenarioSpec spec = spec(167L);
        final ScenarioSummary summary = runner.run(spec);
        final SimulatorHealthState health = new SimulatorHealthState();
        health.markFailed("VenueBehaviorSimulator", "forced failure", 99L);

        assertNull(runner.optimizerInputLineage(summary, spec, health));
    }

    private static ScenarioSpec spec(final long seed) {
        return new ScenarioSpec("lineage-nvt", seed, 6, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 1, 0),
                        new ScenarioWindow(2, 3, 1),
                        new ScenarioWindow(4, 5, 2)
                },
                true,
                ScenarioSimulatorConfig.defaults());
    }
}
