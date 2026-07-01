package com.nitroj.sor.core.optimizer.batch;

import java.util.Optional;

/**
 * Responsibility: retain the latest approved Phase 6 batch allocation plan.
 */
public final class BatchAllocationPlanStore {
    private BatchVenueAllocationPlan latestApproved;
    private BatchAllocationRunMetadata latestAttemptMetadata;

    public synchronized BatchVenueAllocationPlan approve(
            final BatchVenueAllocationPlan plan,
            final BatchAllocationRunMetadata metadata
    ) {
        if (plan == null || metadata == null) {
            throw new IllegalArgumentException("plan and metadata must not be null");
        }
        if (!plan.constraintReport().feasible()) {
            throw new IllegalArgumentException("approved batch allocation plan must be feasible");
        }
        metadata.success = true;
        metadata.failureReason = "";
        metadata.backendStatus = metadata.backendStatus == null ? BatchAllocationStatus.OK : metadata.backendStatus;
        latestApproved = plan;
        latestAttemptMetadata = metadata;
        return latestApproved;
    }

    public synchronized void recordFailure(final BatchAllocationRunMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        metadata.success = false;
        metadata.failureReason = metadata.failureReason == null ? "" : metadata.failureReason;
        metadata.backendStatus = metadata.backendStatus == null ? BatchAllocationStatus.NATIVE_FAILURE : metadata.backendStatus;
        latestAttemptMetadata = metadata;
    }

    public synchronized Optional<BatchVenueAllocationPlan> latestApproved() {
        return Optional.ofNullable(latestApproved);
    }

    public synchronized Optional<BatchVenueAllocationPlan> latestApplicable(final long inputSnapshotId) {
        if (latestApproved == null || latestApproved.inputSnapshotId() != inputSnapshotId) {
            return Optional.empty();
        }
        return Optional.of(latestApproved);
    }

    public synchronized Optional<BatchAllocationRunMetadata> latestAttemptMetadata() {
        return Optional.ofNullable(latestAttemptMetadata);
    }
}
