package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLint;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidateSet;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionConfig;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidator;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Builds the Phase 7 candidate set without publishing it.
     *
     * <p>The existing {@link #runCycle(PolicyOptimizationInput, long)} method is
     * intentionally left as the single-candidate Phase 1-6 path.</p>
     */
    public PolicyCandidateSet buildCandidateSet(
            final PolicyOptimizationInput input,
            final RobustSelectionConfig robustConfig
    ) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        final RobustSelectionConfig config = robustConfig == null ? RobustSelectionConfig.defaults() : robustConfig;
        final StrategicVenueSubsetResult strategic = strategicOptimizer.optimize(input);
        final TacticalPolicyResult tactical = tacticalOptimizer.optimize(strategic, input);
        final List<PolicyCandidate> candidates = new ArrayList<>();
        int ordinal = 0;
        for (int riskScale : config.candidateGrid().riskScaleBps()) {
            for (int concentrationScale : config.candidateGrid().concentrationPenaltyScaleBps()) {
                if (ordinal >= config.candidateGrid().maxCandidateCount()) {
                    return new PolicyCandidateSet(candidates);
                }
                final MutablePolicyCandidate candidate = baseCandidate(input, strategic, tactical);
                perturbCandidate(candidate, ordinal, riskScale, concentrationScale);
                final PolicyLintReport lintReport = lint.lint(candidate, input, strategic);
                if (lintReport.hasErrors()) {
                    continue;
                }
                final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lintReport);
                if (containsHash(candidates, policy.policyHash64)) {
                    continue;
                }
                candidates.add(new PolicyCandidate(
                        candidates.size(),
                        "riskScaleBps=" + riskScale + ",concentrationPenaltyScaleBps=" + concentrationScale,
                        candidate,
                        policy
                ));
                ordinal++;
            }
        }
        return new PolicyCandidateSet(candidates);
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

    private MutablePolicyCandidate baseCandidate(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult strategic,
            final TacticalPolicyResult tactical
    ) {
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(
                input.instrumentCount,
                input.venueCount,
                input.regimeCount,
                input.urgencyCount
        );
        tacticalOptimizer.applyToCandidate(candidate, strategic, input, tactical);
        return candidate;
    }

    private static void perturbCandidate(
            final MutablePolicyCandidate candidate,
            final int ordinal,
            final int riskScaleBps,
            final int concentrationPenaltyScaleBps
    ) {
        final int riskDelta = riskScaleBps - 10_000;
        final int concentrationDelta = concentrationPenaltyScaleBps - 10_000;
        for (int i = 0; i < candidate.venueEligible.length; i++) {
            if (!candidate.venueEligible[i]) {
                continue;
            }
            final int venueId = (i / candidate.urgencyCount / candidate.regimeCount) % candidate.venueCount;
            final int directionalTilt = (ordinal + 1) * (venueId + 1) * 25;
            candidate.venueWeightBps[i] = clampBps(candidate.venueWeightBps[i] + directionalTilt + riskDelta / 20);
            candidate.toxicityPenaltyBps[i] = clampBps(candidate.toxicityPenaltyBps[i] + Math.max(0, concentrationDelta / 40));
        }
    }

    private static boolean containsHash(final List<PolicyCandidate> candidates, final long hash64) {
        for (PolicyCandidate candidate : candidates) {
            if (candidate.canonicalPolicyHash64() == hash64) {
                return true;
            }
        }
        return false;
    }

    private static int clampBps(final int value) {
        return Math.max(0, Math.min(10_000, value));
    }
}
