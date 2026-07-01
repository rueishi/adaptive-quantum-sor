package com.nitroj.sor.core.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 4 completion report artifact.
 *
 * <p>Role in system: covers P4-TC-005 documentation evidence for model
 * artifact validation, optimizer lineage, failure fallback, and test coverage.</p>
 *
 * <p>Relationships: reads {@code docs/reports/phase-1-7/PHASE_4_COMPLETION_REPORT.md} as a
 * build-time documentation check.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: keep Phase 4 readiness evidence tied to source-controlled
 * files and tests instead of relying on manual report inspection.</p>
 */
final class Phase4CompletionReportTest {
    private static final Path REPORT = Path.of("docs", "reports", "phase-1-7", "PHASE_4_COMPLETION_REPORT.md");

    @Test
    void reportContainsModelPipelineAndFailureEvidence() throws IOException {
        final String markdown = Files.readString(REPORT, StandardCharsets.UTF_8);

        assertContains(markdown, "# Phase 4 Completion Report");
        assertContains(markdown, "P4-ML-001");
        assertContains(markdown, "P4-ML-009");
        assertContains(markdown, "X-DOC-001");
        assertContains(markdown, "None known for Phase 4.");
        assertContains(markdown, "ModelArtifactImporter.java");
        assertContains(markdown, "ModelSignalValidator.java");
        assertContains(markdown, "PolicyOptimizationInput.java");
        assertContains(markdown, "model_metadata.properties");
        assertContains(markdown, "predictions.csv");
        assertContains(markdown, "validation_metrics.csv");
        assertContains(markdown, "previous approved `ModelSignalState`");
        assertContains(markdown, "modelSignalVersion");
        assertContains(markdown, "FeatureDatasetExporterTest");
        assertContains(markdown, "PythonTrainingPipelineTest");
        assertContains(markdown, "ModelArtifactImporterTest");
        assertContains(markdown, "ScenarioOptimizerLineageIntegrationTest");
    }

    private static void assertContains(final String markdown, final String expected) {
        assertTrue(markdown.contains(expected), () -> "report must contain: " + expected);
    }
}
