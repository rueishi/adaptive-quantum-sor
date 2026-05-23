package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: represent backend status and optional batch plan.
 */
public record BatchAllocationBackendResult(
        BatchAllocationStatus status,
        BatchVenueAllocationPlan plan,
        String message
) {
    public BatchAllocationBackendResult {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        message = message == null ? "" : message;
    }

    public static BatchAllocationBackendResult success(final BatchVenueAllocationPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        return new BatchAllocationBackendResult(BatchAllocationStatus.OK, plan, "OK");
    }

    public static BatchAllocationBackendResult failure(final BatchAllocationStatus status, final String message) {
        if (status == BatchAllocationStatus.OK) {
            throw new IllegalArgumentException("failure status must not be OK");
        }
        return new BatchAllocationBackendResult(status, null, message);
    }
}
