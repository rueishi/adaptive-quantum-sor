package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 6 batch allocation completion documentation evidence.
 */
final class Phase6CompletionReportTest {
    @Test
    void phase6CompletionReportListsImplementedAcsScenariosAndCommands() throws IOException {
        final String report = Files.readString(Path.of("docs/PHASE_6_COMPLETION_REPORT.md"), StandardCharsets.UTF_8);

        for (String expected : new String[]{
                "P6-BATCH-001",
                "P6-BATCH-010",
                "batch_same_venue_self_impact.yaml",
                "batch_shared_capacity.yaml",
                "batch_correlated_venue_leakage.yaml",
                "batch_infeasible_fallback.yaml",
                "same-venue self-impact changes allocation",
                "correlated venue leakage changes allocation",
                "Failed ACs",
                "none",
                "./gradlew test --tests com.nitroj.adaptive.quantum.sor.optimizer.batch.*",
                "scripts/run_tests.sh scenario",
                "scripts/run_tests.sh all"
        }) {
            assertTrue(report.contains(expected), () -> "Phase 6 report must contain: " + expected);
        }
    }
}
