package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: record one Phase 6 batch allocation attempt.
 */
public final class BatchAllocationRunMetadata {
    public long batchAllocationRunId;
    public long inputSnapshotId;
    public long startedAtNanos;
    public long completedAtNanos;
    public boolean success;
    public String failureReason = "";
    public BatchAllocationStatus backendStatus = BatchAllocationStatus.OK;
}
