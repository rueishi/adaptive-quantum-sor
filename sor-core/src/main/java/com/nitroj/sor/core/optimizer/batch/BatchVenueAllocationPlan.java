package com.nitroj.sor.core.optimizer.batch;

import java.util.Arrays;

/**
 * Responsibility: carry an approved parent x venue batch allocation plan.
 */
public final class BatchVenueAllocationPlan {
    private final long batchAllocationRunId;
    private final long inputSnapshotId;
    private final long createdAtNanos;
    private final long objectiveCost;
    private final BatchAllocationConstraintReport constraintReport;
    private final long[][] parentVenueQuantities;

    public BatchVenueAllocationPlan(
            final long batchAllocationRunId,
            final long inputSnapshotId,
            final long createdAtNanos,
            final long objectiveCost,
            final BatchAllocationConstraintReport constraintReport,
            final long[][] parentVenueQuantities
    ) {
        if (batchAllocationRunId <= 0L) {
            throw new IllegalArgumentException("batchAllocationRunId must be positive");
        }
        if (inputSnapshotId <= 0L) {
            throw new IllegalArgumentException("inputSnapshotId must be positive");
        }
        if (constraintReport == null) {
            throw new IllegalArgumentException("constraintReport must not be null");
        }
        if (parentVenueQuantities == null || parentVenueQuantities.length == 0) {
            throw new IllegalArgumentException("parentVenueQuantities must not be empty");
        }
        this.batchAllocationRunId = batchAllocationRunId;
        this.inputSnapshotId = inputSnapshotId;
        this.createdAtNanos = createdAtNanos;
        this.objectiveCost = objectiveCost;
        this.constraintReport = constraintReport;
        this.parentVenueQuantities = copy(parentVenueQuantities);
    }

    public long batchAllocationRunId() {
        return batchAllocationRunId;
    }

    public long inputSnapshotId() {
        return inputSnapshotId;
    }

    public long createdAtNanos() {
        return createdAtNanos;
    }

    public long objectiveCost() {
        return objectiveCost;
    }

    public BatchAllocationConstraintReport constraintReport() {
        return constraintReport;
    }

    public long[][] parentVenueQuantities() {
        return copy(parentVenueQuantities);
    }

    private static long[][] copy(final long[][] source) {
        final long[][] copy = new long[source.length][];
        int width = -1;
        for (int row = 0; row < source.length; row++) {
            if (source[row] == null || source[row].length == 0) {
                throw new IllegalArgumentException("parentVenueQuantities rows must not be empty");
            }
            if (width < 0) {
                width = source[row].length;
            } else if (source[row].length != width) {
                throw new IllegalArgumentException("parentVenueQuantities must be rectangular");
            }
            copy[row] = Arrays.copyOf(source[row], source[row].length);
            for (long value : copy[row]) {
                if (value < 0L) {
                    throw new IllegalArgumentException("parentVenueQuantities values must be non-negative");
                }
            }
        }
        return copy;
    }
}
