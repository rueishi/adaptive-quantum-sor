package com.nitroj.sor.core.optimizer.batch;

/**
 * Responsibility: render user-facing evidence for one batch allocation plan.
 */
public final class BatchAllocationReport {
    private BatchAllocationReport() {
    }

    public static String markdown(final BatchVenueAllocationPlan plan, final String fallbackStatus) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        final StringBuilder out = new StringBuilder();
        out.append("# Batch Venue Allocation Report\n\n");
        out.append("| Field | Value |\n");
        out.append("|---|---:|\n");
        out.append("| batchAllocationRunId | ").append(plan.batchAllocationRunId()).append(" |\n");
        out.append("| inputSnapshotId | ").append(plan.inputSnapshotId()).append(" |\n");
        out.append("| objectiveCost | ").append(plan.objectiveCost()).append(" |\n");
        out.append("| parentResidualQty | ").append(plan.constraintReport().parentResidualQty()).append(" |\n");
        out.append("| maxVenueCapacityExcess | ").append(plan.constraintReport().maxVenueCapacityExcess()).append(" |\n");
        out.append("| maxParticipationExcess | ").append(plan.constraintReport().maxParticipationExcess()).append(" |\n");
        out.append("| fallbackStatus | ").append(fallbackStatus == null ? "" : fallbackStatus).append(" |\n\n");
        out.append("## Parent Venue Quantities\n\n");
        out.append("| Parent Index | Venue Index | Quantity |\n");
        out.append("|---:|---:|---:|\n");
        final long[][] quantities = plan.parentVenueQuantities();
        for (int parent = 0; parent < quantities.length; parent++) {
            for (int venue = 0; venue < quantities[parent].length; venue++) {
                out.append("| ").append(parent)
                        .append(" | ").append(venue)
                        .append(" | ").append(quantities[parent][venue])
                        .append(" |\n");
            }
        }
        return out.toString();
    }
}
