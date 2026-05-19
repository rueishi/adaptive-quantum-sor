package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLint;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidator;

/**
 * Responsibility: orchestrate one warm-path policy optimization cycle.
 *
 * <p>Role in system: this coordinator links model/strategic/tactical optimizer
 * output to lint, compile, validate, and publish steps.</p>
 *
 * <p>Relationships: consumes optimizer stubs, policy lint, compiler, validator,
 * and publisher components created by previous task cards.</p>
 *
 * <p>Lifecycle: called by a scheduler or test harness. It records input
 * snapshot lineage for every run and never participates in execution routing.</p>
 *
 * <p>Design intent: expected optimizer failures return a failed cycle result
 * and leave the active policy untouched.</p>
 */
public final class PolicyOptimizerCoordinator {
    private final OptimizerCadenceConfig cadenceConfig;
    private final StrategicVenueSubsetOptimizer strategicOptimizer;
    private final CudaTacticalOptimizerStub tacticalOptimizer;
    private final PolicyLint lint;
    private final DefaultPolicyCompiler compiler;
    private final PolicyValidator validator;
    private final PolicyPublisher publisher;
    private long lastRunNanos = Long.MIN_VALUE;

    public PolicyOptimizerCoordinator(
            final OptimizerCadenceConfig cadenceConfig,
            final StrategicVenueSubsetOptimizer strategicOptimizer,
            final CudaTacticalOptimizerStub tacticalOptimizer,
            final PolicyLint lint,
            final DefaultPolicyCompiler compiler,
            final PolicyValidator validator,
            final PolicyPublisher publisher
    ) {
        if (cadenceConfig == null || strategicOptimizer == null || tacticalOptimizer == null || lint == null
                || compiler == null || validator == null || publisher == null) {
            throw new IllegalArgumentException("coordinator dependencies must not be null");
        }
        this.cadenceConfig = cadenceConfig;
        this.strategicOptimizer = strategicOptimizer;
        this.tacticalOptimizer = tacticalOptimizer;
        this.lint = lint;
        this.compiler = compiler;
        this.validator = validator;
        this.publisher = publisher;
    }

    /**
     * Runs a cycle if cadence permits, recording snapshot metadata even when an
     * optimizer fails.
     */
    public OptimizerCycleResult runCycle(final PolicyOptimizationInput input, final long nowNanos) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (lastRunNanos != Long.MIN_VALUE && nowNanos - lastRunNanos < cadenceConfig.cycleIntervalNanos) {
            return new OptimizerCycleResult(false, false, metadata(input, nowNanos), null, "cadence_not_elapsed");
        }
        lastRunNanos = nowNanos;
        final OptimizerRunMetadata metadata = metadata(input, nowNanos);
        try {
            final StrategicVenueSubsetResult strategic = strategicOptimizer.optimize(input);
            final TacticalPolicyResult tactical = tacticalOptimizer.optimize(strategic, input);
            final MutablePolicyCandidate candidate = new MutablePolicyCandidate(input.instrumentCount, input.venueCount, input.regimeCount, input.urgencyCount);
            tacticalOptimizer.applyToCandidate(candidate, strategic, input, tactical);
            final PolicyLintReport lintReport = lint.lint(candidate, input, strategic);
            if (lintReport.hasErrors()) {
                metadata.success = false;
                metadata.failureReason = "lint_failed";
                return new OptimizerCycleResult(true, false, metadata, null, metadata.failureReason);
            }
            final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lintReport);
            final PolicyValidationReport validation = validator.validate(policy);
            final var publication = publisher.publish(policy, validation, lintReport, compiler.lastDiff(), 10, nowNanos);
            metadata.success = publication.publishAllowed;
            metadata.completedAtNanos = nowNanos;
            return new OptimizerCycleResult(true, publication.publishAllowed, metadata, publication, publication.publishAllowed ? null : "publication_rejected");
        } catch (RuntimeException ex) {
            metadata.success = false;
            metadata.completedAtNanos = nowNanos;
            metadata.failureReason = ex.getMessage();
            return new OptimizerCycleResult(true, false, metadata, null, metadata.failureReason);
        }
    }

    private static OptimizerRunMetadata metadata(final PolicyOptimizationInput input, final long nowNanos) {
        final OptimizerRunMetadata metadata = new OptimizerRunMetadata();
        metadata.optimizerRunId = input.inputSnapshotId;
        metadata.startedAtNanos = nowNanos;
        metadata.completedAtNanos = nowNanos;
        metadata.inputSnapshotId = input.inputSnapshotId;
        metadata.marketDataSnapshotSeq = input.marketDataSnapshotSeq;
        metadata.venueStatsSnapshotSeq = input.venueStatsSnapshotSeq;
        metadata.modelSignalVersion = input.modelSignalVersion;
        metadata.currentPolicyVersion = input.currentPolicyVersion;
        return metadata;
    }
}
