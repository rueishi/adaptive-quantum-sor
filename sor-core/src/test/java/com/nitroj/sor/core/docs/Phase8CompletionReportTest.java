package com.nitroj.sor.core.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies Phase 8 completion evidence and non-deferred card reports. */
final class Phase8CompletionReportTest {
    private static final String[] REQUIRED_CARDS = {
            "P8-01", "P8-02", "P8-03", "P8-04", "P8-05", "P8-06", "P8-07",
            "P8-08", "P8-09", "P8-10", "P8-13", "P8-14", "P8-15",
            "P8-23", "P8-24", "P8-25", "P8-26", "P8-27", "P8-28", "P8-29", "P8-30",
            "P8-20", "P8-21", "P8-22"
    };

    @Test
    void phase8CompletionReportListsNonDeferredCards() throws IOException {
        final String report = read("docs/reports/phase-8/PHASE_8_COMPLETION_REPORT.md");

        assertContains(report, "Phase 8 framework extraction is complete");
        assertContains(report, "P8-12 remains deferred");
        assertContains(report, "P8-23 through P8-30");
        assertContains(report, "Failed ACs");
        assertContains(report, "none");

        for (final String card : REQUIRED_CARDS) {
            assertContains(report, card);
        }
    }

    @Test
    void everyNonDeferredCardHasCompletionEvidence() throws IOException {
        for (final String card : REQUIRED_CARDS) {
            final Path path = Path.of("docs", "reports", "phase-8", "PHASE_8_" + card + "_REPORT.md");
            assertTrue(Files.isRegularFile(path), () -> "missing Phase 8 card report: " + path);
            final String report = Files.readString(path, StandardCharsets.UTF_8);
            assertContains(report, "Implemented Scope");
            assertContains(report, "Acceptance Criteria Evidence");
            assertContains(report, "Implemented ACs");
            assertContains(report, "Planned ACs");
            assertContains(report, "Failed ACs");
            assertContains(report, "none");
            assertContains(report, "Validation Commands");
        }
    }

    private static String read(final String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static void assertContains(final String text, final String expected) {
        assertTrue(text.contains(expected), () -> "Phase 8 report evidence must contain: " + expected);
    }
}
