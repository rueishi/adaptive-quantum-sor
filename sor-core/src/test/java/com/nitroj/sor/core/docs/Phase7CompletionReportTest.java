package com.nitroj.sor.core.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 7 robust-selection completion documentation evidence.
 */
final class Phase7CompletionReportTest {
    @Test
    void phase7CompletionReportListsImplementedAcsLimitsAndCommands() throws IOException {
        final String report = Files.readString(Path.of("docs/reports/phase-1-7/PHASE_7_COMPLETION_REPORT.md"), StandardCharsets.UTF_8);

        for (String expected : new String[]{
                "P7-ROBUST-001",
                "P7-ROBUST-012",
                "X-AUDIT-001",
                "X-OBS-001",
                "X-DOC-001",
                "robustSelection.enabled=false",
                "ScenarioScorecardV1",
                "ScoreMatrix",
                "RobustPublicationGate",
                "Failed ACs",
                "none",
                "./gradlew test --tests com.nitroj.sor.core.policy.robust.*",
                "scripts/run_tests.sh all",
                "Known Limits"
        }) {
            assertTrue(report.contains(expected), () -> "Phase 7 report must contain: " + expected);
        }
    }
}
