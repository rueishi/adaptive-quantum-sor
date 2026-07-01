package com.nitroj.sor.core.optimizer.batch;

import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify approved Phase 6 plans are safely retained.
 */
final class BatchAllocationPlanStoreTest {
    private final DeterministicBatchVenueAllocator allocator = new DeterministicBatchVenueAllocator();

    @Test
    void approvedPlanIsRetainedAndCanBeAttachedToOptimizerInput() {
        final BatchAllocationPlanStore store = new BatchAllocationPlanStore();
        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());

        store.approve(plan, metadata(plan));
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = plan.inputSnapshotId();
        input.latestBatchAllocationPlan = store.latestApplicable(input.inputSnapshotId).orElseThrow();

        assertSame(plan, store.latestApproved().orElseThrow());
        assertSame(plan, input.latestBatchAllocationPlan);
        assertTrue(store.latestAttemptMetadata().orElseThrow().success);
    }

    @Test
    void failedAttemptDoesNotReplaceLatestApprovedPlan() {
        final BatchAllocationPlanStore store = new BatchAllocationPlanStore();
        final BatchVenueAllocationPlan approved = allocator.allocate(BatchAllocationProblemTest.problem());
        store.approve(approved, metadata(approved));

        final BatchAllocationRunMetadata failure = new BatchAllocationRunMetadata();
        failure.batchAllocationRunId = 99L;
        failure.inputSnapshotId = 100L;
        failure.failureReason = "backend unavailable";
        failure.backendStatus = BatchAllocationStatus.BACKEND_UNAVAILABLE;
        store.recordFailure(failure);

        assertSame(approved, store.latestApproved().orElseThrow());
        assertFalse(store.latestAttemptMetadata().orElseThrow().success);
        assertEquals("backend unavailable", store.latestAttemptMetadata().orElseThrow().failureReason);
        assertEquals(BatchAllocationStatus.BACKEND_UNAVAILABLE, store.latestAttemptMetadata().orElseThrow().backendStatus);
    }

    @Test
    void stalePlanIsNotApplicableToDifferentInputSnapshot() {
        final BatchAllocationPlanStore store = new BatchAllocationPlanStore();
        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());
        store.approve(plan, metadata(plan));

        assertTrue(store.latestApplicable(plan.inputSnapshotId()).isPresent());
        assertTrue(store.latestApplicable(plan.inputSnapshotId() + 1L).isEmpty());
    }

    @Test
    void infeasiblePlanCannotBeApproved() {
        final BatchVenueAllocationPlan infeasible = new BatchVenueAllocationPlan(
                1L,
                1L,
                1L,
                0L,
                new BatchAllocationConstraintReport(100, 0, 0),
                new long[][]{{0}}
        );

        assertEquals("approved batch allocation plan must be feasible", assertThrows(IllegalArgumentException.class,
                () -> new BatchAllocationPlanStore().approve(infeasible, metadata(infeasible))
        ).getMessage());
    }

    @Test
    void coordinatorRecordsFailureWithoutReplacingApprovedPlan() {
        final BatchAllocationPlanStore store = new BatchAllocationPlanStore();
        final BatchVenueAllocationPlan approved = allocator.allocate(BatchAllocationProblemTest.problem());
        store.approve(approved, metadata(approved));
        final BatchAllocationCoordinator coordinator = new BatchAllocationCoordinator(
                problem -> {
                    throw new IllegalStateException("no feasible batch allocation");
                },
                store
        );

        assertThrows(IllegalStateException.class, () -> coordinator.optimize(BatchAllocationProblemTest.problem()));

        assertSame(approved, store.latestApproved().orElseThrow());
        assertFalse(store.latestAttemptMetadata().orElseThrow().success);
        assertEquals("no feasible batch allocation", store.latestAttemptMetadata().orElseThrow().failureReason);
    }

    private static BatchAllocationRunMetadata metadata(final BatchVenueAllocationPlan plan) {
        final BatchAllocationRunMetadata metadata = new BatchAllocationRunMetadata();
        metadata.batchAllocationRunId = plan.batchAllocationRunId();
        metadata.inputSnapshotId = plan.inputSnapshotId();
        metadata.startedAtNanos = plan.createdAtNanos();
        metadata.completedAtNanos = plan.createdAtNanos();
        return metadata;
    }
}
