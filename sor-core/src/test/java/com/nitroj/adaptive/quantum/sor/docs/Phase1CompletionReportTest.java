package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 1 completion report artifact.
 *
 * <p>Role in system: P1-TC-028 is documentation-owned and requires a report
 * containing implemented ACs, incomplete ACs, limitations, benchmark results,
 * narrative logs, Jupyter interaction, and static/adaptive comparison output.</p>
 *
 * <p>Relationships: reads {@code docs/PHASE_1_COMPLETION_REPORT.md} as a
 * build-time artifact check.</p>
 *
 * <p>Lifecycle: executed by Gradle with the rest of the JUnit suite.</p>
 *
 * <p>Design intent: keep the manual-review document from being accidentally
 * removed or stripped of required sections.</p>
 */
final class Phase1CompletionReportTest {
    private static final Path REPORT = Path.of("docs", "PHASE_1_COMPLETION_REPORT.md");

    @Test
    void reportContainsRequiredPhaseOneReadinessSections() throws IOException {
        final String markdown = Files.readString(REPORT, StandardCharsets.UTF_8);

        assertContains(markdown, "# Phase 1 Completion Report");
        assertContains(markdown, "## Implemented Acceptance Criteria");
        assertContains(markdown, "## Incomplete Acceptance Criteria");
        assertContains(markdown, "## Known Limitations");
        assertContains(markdown, "## Benchmark Results");
        assertContains(markdown, "## Sample Narrative Log");
        assertContains(markdown, "## Sample Jupyter Interaction");
        assertContains(markdown, "## Static vs Adaptive Comparison");
        assertContains(markdown, "X-DOC-001");
        assertContains(markdown, "X-CONFIG-001");
        assertContains(markdown, "X-DET-001");
        assertContains(markdown, "AC-HOTPATH-001");
        assertContains(markdown, "AC-LINT-001");
        assertContains(markdown, "AC-POLICY-001");
        assertContains(markdown, "AC-INDEX-001");
        assertContains(markdown, "AC-COMPARE-001");
        assertContains(markdown, "X-E2E-001");
        assertContains(markdown, "P1-TC-027");
        assertContains(markdown, "SorEndToEndTest");
    }

    private static void assertContains(final String markdown, final String expected) {
        assertTrue(markdown.contains(expected), () -> "report must contain: " + expected);
    }
}
