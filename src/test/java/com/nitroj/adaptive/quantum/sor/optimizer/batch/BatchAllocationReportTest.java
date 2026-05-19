package com.nitroj.adaptive.quantum.sor.optimizer.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify user-facing Phase 6 batch allocation report evidence.
 */
final class BatchAllocationReportTest {
    @Test
    void reportListsObjectiveConstraintsQuantitiesAndFallbackStatus() {
        final BatchVenueAllocationPlan plan = new DeterministicBatchVenueAllocator()
                .allocate(BatchAllocationProblemTest.problem());

        final String markdown = BatchAllocationReport.markdown(plan, "none");

        assertTrue(markdown.contains("# Batch Venue Allocation Report"));
        assertTrue(markdown.contains("objectiveCost"));
        assertTrue(markdown.contains("maxVenueCapacityExcess"));
        assertTrue(markdown.contains("fallbackStatus"));
        assertTrue(markdown.contains("| 0 | 0 | 100 |"));
        assertTrue(markdown.contains("| 1 | 1 | 100 |"));
    }
}
