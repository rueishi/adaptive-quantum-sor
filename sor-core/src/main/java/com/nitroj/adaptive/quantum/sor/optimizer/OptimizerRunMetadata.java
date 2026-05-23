package com.nitroj.adaptive.quantum.sor.optimizer;

/**
 * Responsibility: record lineage and success/failure for one optimizer run.
 *
 * <p>Role in system: optimizer coordination and audit flows use this metadata
 * to explain which input snapshots and policy versions produced a strategic or
 * tactical result.</p>
 *
 * <p>Relationships: produced by optimizer stubs and future native optimizers;
 * referenced by governance and publication records.</p>
 *
 * <p>Lifecycle: allocated per optimizer invocation and treated as immutable by
 * convention after the run completes.</p>
 *
 * <p>Design intent: primitive fields match the authoritative spec and keep
 * lineage easy to serialize in later audit tasks.</p>
 */
public final class OptimizerRunMetadata {
    public long optimizerRunId;
    public int optimizerType;
    public long startedAtNanos;
    public long completedAtNanos;
    public long inputSnapshotId;
    public long marketDataSnapshotSeq;
    public long venueStatsSnapshotSeq;
    public long modelSignalVersion;
    public long currentPolicyVersion;
    public String scenarioId;
    public long scenarioSeed;
    public int scenarioTicks;
    public int scenarioStartTickInclusive;
    public int scenarioEndTickInclusive;
    public boolean success;
    public String failureReason;
}
