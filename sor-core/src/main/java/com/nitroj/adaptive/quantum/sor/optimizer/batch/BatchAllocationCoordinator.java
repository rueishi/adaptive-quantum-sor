package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: run batch allocation attempts and publish only valid plans.
 */
public final class BatchAllocationCoordinator {
    private final BatchVenueAllocator allocator;
    private final BatchAllocationPlanStore store;

    public BatchAllocationCoordinator(final BatchVenueAllocator allocator, final BatchAllocationPlanStore store) {
        if (allocator == null || store == null) {
            throw new IllegalArgumentException("allocator and store must not be null");
        }
        this.allocator = allocator;
        this.store = store;
    }

    public BatchVenueAllocationPlan optimize(final BatchAllocationProblem problem) {
        if (problem == null) {
            throw new IllegalArgumentException("problem must not be null");
        }
        final BatchAllocationRunMetadata metadata = new BatchAllocationRunMetadata();
        metadata.batchAllocationRunId = problem.batchAllocationRunId();
        metadata.inputSnapshotId = problem.inputSnapshotId();
        metadata.startedAtNanos = problem.createdAtNanos();
        try {
            final BatchVenueAllocationPlan plan = allocator.allocate(problem);
            metadata.completedAtNanos = problem.createdAtNanos();
            return store.approve(plan, metadata);
        } catch (RuntimeException ex) {
            metadata.completedAtNanos = problem.createdAtNanos();
            metadata.failureReason = ex.getMessage();
            metadata.backendStatus = BatchAllocationStatus.INVALID_RESULT;
            store.recordFailure(metadata);
            throw ex;
        }
    }

    public BatchAllocationPlanStore store() {
        return store;
    }
}
