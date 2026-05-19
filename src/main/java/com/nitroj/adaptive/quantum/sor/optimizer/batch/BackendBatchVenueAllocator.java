package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: use a swappable backend when valid and fall back safely.
 */
public final class BackendBatchVenueAllocator implements BatchVenueAllocator {
    private final BatchAllocationBackend backend;
    private final DeterministicBatchVenueAllocator reference;

    public BackendBatchVenueAllocator(
            final BatchAllocationBackend backend,
            final DeterministicBatchVenueAllocator reference
    ) {
        if (backend == null || reference == null) {
            throw new IllegalArgumentException("backend and reference must not be null");
        }
        this.backend = backend;
        this.reference = reference;
    }

    @Override
    public BatchVenueAllocationPlan allocate(final BatchAllocationProblem problem) {
        if (problem == null) {
            throw new IllegalArgumentException("problem must not be null");
        }
        final BatchVenueAllocationPlan referencePlan = reference.allocate(problem);
        final BatchAllocationBackendResult backendResult = backend.allocate(problem);
        if (backendResult == null || backendResult.status() != BatchAllocationStatus.OK || backendResult.plan() == null) {
            return referencePlan;
        }
        validateBackendPlan(problem, referencePlan, backendResult.plan());
        return backendResult.plan();
    }

    private static void validateBackendPlan(
            final BatchAllocationProblem problem,
            final BatchVenueAllocationPlan referencePlan,
            final BatchVenueAllocationPlan backendPlan
    ) {
        final BatchAllocationConstraintReport report = problem.evaluate(backendPlan.parentVenueQuantities());
        if (!report.feasible()) {
            throw new IllegalStateException("batch backend returned infeasible allocation");
        }
        final long objective = problem.objectiveCost(backendPlan.parentVenueQuantities());
        if (objective != backendPlan.objectiveCost()) {
            throw new IllegalStateException("batch backend objective does not match allocation");
        }
        if (objective != referencePlan.objectiveCost()) {
            throw new IllegalStateException("batch backend objective does not match reference");
        }
    }
}
