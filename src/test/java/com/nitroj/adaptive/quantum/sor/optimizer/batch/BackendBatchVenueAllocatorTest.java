package com.nitroj.adaptive.quantum.sor.optimizer.batch;

import com.nitroj.adaptive.quantum.sor.nativebridge.BatchAllocatorNativeBridge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 6 swappable backend boundary and fallback.
 */
final class BackendBatchVenueAllocatorTest {
    private final DeterministicBatchVenueAllocator reference = new DeterministicBatchVenueAllocator();

    @Test
    void unavailableNativeBackendFallsBackToReferencePlan() {
        final BatchAllocatorNativeBridge bridge = new BatchAllocatorNativeBridge();
        final BackendBatchVenueAllocator allocator = new BackendBatchVenueAllocator(bridge, reference);

        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());

        assertFalse(bridge.backendAvailable());
        assertEquals(9L, plan.objectiveCost());
        assertArrayEquals(new long[]{100L, 0L}, plan.parentVenueQuantities()[0]);
        assertArrayEquals(new long[]{0L, 100L}, plan.parentVenueQuantities()[1]);
    }

    @Test
    void availableBackendPlanIsAcceptedWhenItMatchesReference() {
        final BatchAllocatorNativeBridge bridge = new BatchAllocatorNativeBridge(reference);
        final BackendBatchVenueAllocator allocator = new BackendBatchVenueAllocator(bridge, reference);

        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());

        assertTrue(bridge.backendAvailable());
        assertEquals(9L, plan.objectiveCost());
    }

    @Test
    void timeoutFallsBackToReferencePlan() {
        final BatchAllocationBackend timeout = problem ->
                BatchAllocationBackendResult.failure(BatchAllocationStatus.TIMEOUT, "timed out");
        final BackendBatchVenueAllocator allocator = new BackendBatchVenueAllocator(timeout, reference);

        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());

        assertEquals(9L, plan.objectiveCost());
    }

    @Test
    void invalidBackendPlanIsRejectedBeforePublication() {
        final BatchAllocationBackend invalid = problem -> BatchAllocationBackendResult.success(
                new BatchVenueAllocationPlan(
                        problem.batchAllocationRunId(),
                        problem.inputSnapshotId(),
                        problem.createdAtNanos(),
                        0L,
                        new BatchAllocationConstraintReport(0, 0, 0),
                        new long[][]{
                                {100, 100},
                                {100, 100}
                        }
                )
        );
        final BackendBatchVenueAllocator allocator = new BackendBatchVenueAllocator(invalid, reference);

        assertEquals("batch backend returned infeasible allocation", assertThrows(IllegalStateException.class,
                () -> allocator.allocate(BatchAllocationProblemTest.problem())
        ).getMessage());
    }

    @Test
    void backendPlanThatDisagreesWithReferenceIsRejected() {
        final BatchAllocationBackend disagreeing = problem -> BatchAllocationBackendResult.success(
                new BatchVenueAllocationPlan(
                        problem.batchAllocationRunId(),
                        problem.inputSnapshotId(),
                        problem.createdAtNanos(),
                        32L,
                        new BatchAllocationConstraintReport(0, 0, 0),
                        new long[][]{
                                {100, 0},
                                {100, 0}
                        }
                )
        );
        final BackendBatchVenueAllocator allocator = new BackendBatchVenueAllocator(disagreeing, reference);

        assertEquals("batch backend objective does not match reference", assertThrows(IllegalStateException.class,
                () -> allocator.allocate(BatchAllocationProblemTest.problem())
        ).getMessage());
    }
}
