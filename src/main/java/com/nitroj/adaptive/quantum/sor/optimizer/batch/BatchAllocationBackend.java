package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: swappable backend boundary for Phase 6 batch allocation.
 */
public interface BatchAllocationBackend {
    BatchAllocationBackendResult allocate(BatchAllocationProblem problem);
}
