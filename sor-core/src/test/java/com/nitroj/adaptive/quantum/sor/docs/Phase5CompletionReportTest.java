package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 5 simulator completion documentation evidence.
 *
 * <p>Role in system: covers P5-TC-006 documentation guards for implemented
 * acceptance criteria, known limits, and reproduction commands.</p>
 *
 * <p>Relationships: reads {@code docs/reports/phase-1-7/PHASE_5_COMPLETION_REPORT.md} and the
 * CI profile documentation.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit with documentation tests.</p>
 *
 * <p>Design intent: make simulator upgrade evidence testable so docs do not
 * drift away from the implemented scenario harness.</p>
 */
final class Phase5CompletionReportTest {
    @Test
    void phase5CompletionReportListsImplementedAcsAndCommands() throws IOException {
        final String report = Files.readString(Path.of("docs/reports/phase-1-7/PHASE_5_COMPLETION_REPORT.md"), StandardCharsets.UTF_8);

        for (String expected : new String[]{
                "P5-SIM-001",
                "P5-SIM-016",
                "X-E2E-001",
                "X-DOC-001",
                "Failed ACs",
                "none",
                "scripts/run_tests.sh simulator",
                "scripts/run_tests.sh scenario",
                "scripts/run_tests.sh integration",
                "scripts/run_tests.sh all"
        }) {
            assertTrue(report.contains(expected), () -> "Phase 5 report must contain: " + expected);
        }
    }
}
