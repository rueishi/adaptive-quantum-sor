package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 3 completion report artifact.
 *
 * <p>Role in system: covers P3-TC-005 documentation evidence for strategic
 * optimizer audit lineage and accepted/rejected result reporting.</p>
 *
 * <p>Relationships: reads {@code docs/PHASE_3_COMPLETION_REPORT.md} as a
 * build-time documentation check.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: keep Phase 3 readiness evidence tied to the build instead
 * of relying on a manually inspected report.</p>
 */
final class Phase3CompletionReportTest {
    private static final Path REPORT = Path.of("docs", "PHASE_3_COMPLETION_REPORT.md");

    @Test
    void reportContainsAuditAndValidationEvidence() throws IOException {
        final String markdown = Files.readString(REPORT, StandardCharsets.UTF_8);

        assertContains(markdown, "# Phase 3 Completion Report");
        assertContains(markdown, "P3-ISING-008");
        assertContains(markdown, "P3-ISING-009");
        assertContains(markdown, "X-DOC-001");
        assertContains(markdown, "None known for Phase 3.");
        assertContains(markdown, "## Audit Lineage");
        assertContains(markdown, "optimizer run ID");
        assertContains(markdown, "objective linear coefficients");
        assertContains(markdown, "objective pair coefficients");
        assertContains(markdown, "selected venue IDs");
        assertContains(markdown, "## Accepted And Rejected Result Evidence");
        assertContains(markdown, "StrategicOptimizerAudit.accepted");
        assertContains(markdown, "StrategicOptimizerAudit.rejected");
        assertContains(markdown, "QuboObjectiveConfigTest");
        assertContains(markdown, "StrategicOptimizerNativeBridgeTest");
        assertContains(markdown, "CudaQStrategicOptimizerTest");
        assertContains(markdown, "CudaQFailureFallbackTest");
        assertContains(markdown, "StrategicOptimizerAuditTest");
        assertContains(markdown, "cudaq_strategic_optimizer_test");
        assertContains(markdown, "pair penalty changes the");
    }

    private static void assertContains(final String markdown, final String expected) {
        assertTrue(markdown.contains(expected), () -> "report must contain: " + expected);
    }
}
