package com.nitroj.adaptive.quantum.sor.governance;

import com.nitroj.adaptive.quantum.sor.optimizer.OptimizerRunMetadata;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.ising.QuboObjectiveConfig;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

import java.util.Arrays;

/**
 * Responsibility: capture audit lineage for one strategic optimizer attempt.
 *
 * <p>Role in system: Phase 3 report and governance flows use this record to
 * explain optimizer run ID, objective configuration, input snapshot lineage,
 * selected subset, and policy diff impact.</p>
 *
 * <p>Relationships: built from {@link OptimizerRunMetadata},
 * {@link QuboObjectiveConfig}, {@link PolicyOptimizationInput},
 * {@link StrategicVenueSubsetResult}, and {@link PolicyDiff}.</p>
 *
 * <p>Lifecycle: allocated after each accepted or rejected strategic optimizer
 * attempt and retained as immutable-by-convention audit evidence.</p>
 *
 * <p>Design intent: primitive copied arrays make the audit record stable even
 * if optimizer result objects are later reused or mutated in tests.</p>
 */
public final class StrategicOptimizerAudit {
    public final long optimizerRunId;
    public final int optimizerType;
    public final boolean accepted;
    public final String rejectionReason;
    public final long inputSnapshotId;
    public final long modelSignalVersion;
    public final long currentPolicyVersion;
    public final int instrumentId;
    public final int regimeId;
    public final int urgencyId;
    public final int minSubsetSize;
    public final int maxSubsetSize;
    public final int[] objectiveLinearCoefficients;
    public final int[] objectivePairCoefficients;
    public final short[] selectedVenueIds;
    public final long strategicResultVersion;
    public final PolicyDiff policyDiff;

    private StrategicOptimizerAudit(
            final OptimizerRunMetadata metadata,
            final QuboObjectiveConfig objective,
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult result,
            final PolicyDiff policyDiff,
            final boolean accepted,
            final String rejectionReason
    ) {
        if (metadata == null || objective == null || input == null) {
            throw new IllegalArgumentException("metadata, objective, and input must not be null");
        }
        this.optimizerRunId = metadata.optimizerRunId;
        this.optimizerType = metadata.optimizerType;
        this.accepted = accepted;
        this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
        this.inputSnapshotId = input.inputSnapshotId;
        this.modelSignalVersion = input.modelSignalVersion;
        this.currentPolicyVersion = input.currentPolicyVersion;
        this.instrumentId = objective.instrumentId();
        this.regimeId = objective.regimeId();
        this.urgencyId = objective.urgencyId();
        this.minSubsetSize = objective.minSubsetSize();
        this.maxSubsetSize = objective.maxSubsetSize();
        this.objectiveLinearCoefficients = objective.linearCoefficients();
        this.objectivePairCoefficients = objective.pairCoefficients();
        this.selectedVenueIds = result == null || result.selectedVenueIds == null
                ? new short[0]
                : Arrays.copyOf(result.selectedVenueIds, result.selectedVenueIds.length);
        this.strategicResultVersion = result == null ? 0L : result.version;
        this.policyDiff = policyDiff;
    }

    public static StrategicOptimizerAudit accepted(
            final OptimizerRunMetadata metadata,
            final QuboObjectiveConfig objective,
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult result,
            final PolicyDiff policyDiff
    ) {
        if (result == null) {
            throw new IllegalArgumentException("accepted result must not be null");
        }
        return new StrategicOptimizerAudit(metadata, objective, input, result, policyDiff, true, "");
    }

    public static StrategicOptimizerAudit rejected(
            final OptimizerRunMetadata metadata,
            final QuboObjectiveConfig objective,
            final PolicyOptimizationInput input,
            final String rejectionReason
    ) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("rejectionReason must not be blank");
        }
        return new StrategicOptimizerAudit(metadata, objective, input, null, null, false, rejectionReason);
    }

    public String selectedSubsetExplanation() {
        if (!accepted) {
            return "rejected: " + rejectionReason;
        }
        return "accepted strategicResultVersion=" + strategicResultVersion
                + " selectedVenueIds=" + Arrays.toString(selectedVenueIds);
    }
}
