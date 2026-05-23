package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: summarize feasibility of one batch allocation.
 */
public record BatchAllocationConstraintReport(
        long parentResidualQty,
        long maxVenueCapacityExcess,
        long maxParticipationExcess
) {
    public BatchAllocationConstraintReport {
        if (parentResidualQty < 0L || maxVenueCapacityExcess < 0L || maxParticipationExcess < 0L) {
            throw new IllegalArgumentException("constraint report values must be non-negative");
        }
    }

    public boolean feasible() {
        return parentResidualQty == 0L && maxVenueCapacityExcess == 0L && maxParticipationExcess == 0L;
    }
}
