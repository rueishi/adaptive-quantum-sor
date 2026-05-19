package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 2 completion report artifact.
 *
 * <p>Role in system: P2-TC-009 owns the final completion report and this guard
 * keeps the completion evidence visible in the build.</p>
 *
 * <p>Relationships: reads {@code docs/PHASE_2_COMPLETION_REPORT.md} as a
 * build-time documentation check.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: prevent Phase 2 completion evidence from drifting out of
 * the repository.</p>
 */
final class Phase2CompletionReportTest {
    private static final Path REPORT = Path.of("docs", "PHASE_2_COMPLETION_REPORT.md");

    @Test
    void reportContainsRequiredPhaseTwoSections() throws IOException {
        final String markdown = Files.readString(REPORT, StandardCharsets.UTF_8);

        assertContains(markdown, "# Phase 2 Completion Report");
        assertContains(markdown, "## Implemented Acceptance Criteria");
        assertContains(markdown, "P2-CUDA-009");
        assertContains(markdown, "X-DOC-001");
        assertContains(markdown, "## Failed Acceptance Criteria");
        assertContains(markdown, "None known for Phase 2.");
        assertContains(markdown, "## Task Card Evidence");
        assertContains(markdown, "P2-TC-005");
        assertContains(markdown, "P2-TC-006");
        assertContains(markdown, "P2-TC-007");
        assertContains(markdown, "P2-TC-008");
        assertContains(markdown, "P2-TC-009");
        assertContains(markdown, "## Benchmark And Profiling Summary");
        assertContains(markdown, "## Native Diagnostics");
        assertContains(markdown, "## Known Limitations");
        assertContains(markdown, "Java-to-native shared-library integration evidence");
        assertContains(markdown, "Phase 2 integration evidence");
        assertContains(markdown, "Reusable E2E evidence");
        assertContains(markdown, "CMake/CTest");
        assertContains(markdown, "tactical_optimizer_api_test");
        assertContains(markdown, "tactical_optimizer_layout_test");
        assertContains(markdown, "cuda_tactical_optimizer_test");
        assertContains(markdown, "JniTacticalOptimizerNativeBridgeTest");
        assertContains(markdown, "CudaTacticalOptimizerIntegrationTest");
        assertContains(markdown, "SorEndToEndTest Phase 2");
        assertDoesNotContain(markdown, "Still required before final Phase 2 readiness");
        assertDoesNotContain(markdown, "Pending after the Phase 2 test-coverage amendment");
    }

    private static void assertContains(final String markdown, final String expected) {
        assertTrue(markdown.contains(expected), () -> "report must contain: " + expected);
    }

    private static void assertDoesNotContain(final String markdown, final String unexpected) {
        assertFalse(markdown.contains(unexpected), () -> "report must not contain: " + unexpected);
    }
}
