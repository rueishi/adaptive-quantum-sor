package com.nitroj.sor.core.optimizer.batch;

/**
 * Responsibility: solve one cross-parent batch venue allocation problem.
 */
public interface BatchVenueAllocator {
    BatchVenueAllocationPlan allocate(BatchAllocationProblem problem);
}
