package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: solve one cross-parent batch venue allocation problem.
 */
public interface BatchVenueAllocator {
    BatchVenueAllocationPlan allocate(BatchAllocationProblem problem);
}
