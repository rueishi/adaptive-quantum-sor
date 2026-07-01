package com.nitroj.sor.core.policy.lint;

import com.nitroj.sor.core.metadata.OrderTypeCapabilityMatrix;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;

/**
 * Responsibility: implement Phase 1 MVP policy candidate lint checks.
 *
 * <p>Role in system: this class rejects malformed mutable candidates before
 * compiler or publisher logic can convert them into active policy artifacts.</p>
 *
 * <p>Relationships: reads {@link MutablePolicyCandidate},
 * {@link PolicyOptimizationInput}, strategic subsets, and venue capability
 * metadata.</p>
 *
 * <p>Lifecycle: instantiated with immutable lint config and called per
 * candidate. It has no internal mutable state.</p>
 *
 * <p>Design intent: catch structural and bounded-value defects deterministically
 * while converting unexpected exceptions into a blocking lint report.</p>
 */
public final class DefaultPolicyLint implements PolicyLint {
    private final PolicyLintConfig config;

    public DefaultPolicyLint(final PolicyLintConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /**
     * Runs dimension, universe, bounds, child-size, and capability checks.
     */
    @Override
    public PolicyLintReport lint(
            final MutablePolicyCandidate candidate,
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult subset
    ) {
        final PolicyLintIssueCollector collector = new PolicyLintIssueCollector();
        try {
            lintInternal(candidate, input, subset, collector);
        } catch (RuntimeException ex) {
            collector.error(PolicyLintCode.LINT_EXCEPTION, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
        final PolicyLintReport report = collector.report();
        if (!config.allowWarnings && report.hasWarnings()) {
            final PolicyLintIssueCollector strictCollector = new PolicyLintIssueCollector();
            for (PolicyLintIssue issue : report.issues()) {
                if (issue.severity() == PolicyLintSeverity.WARNING) {
                    strictCollector.error(issue.code(), issue.message());
                } else if (issue.severity() == PolicyLintSeverity.ERROR) {
                    strictCollector.error(issue.code(), issue.message());
                }
            }
            return strictCollector.report();
        }
        return report;
    }

    private void lintInternal(
            final MutablePolicyCandidate candidate,
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult subset,
            final PolicyLintIssueCollector collector
    ) {
        if (candidate == null || input == null) {
            throw new IllegalArgumentException("candidate and input must not be null");
        }
        final int expected = candidate.instrumentCount * candidate.venueCount * candidate.regimeCount * candidate.urgencyCount;
        checkLength("venueEligible", candidate.venueEligible.length, expected, collector);
        checkLength("venueWeightBps", candidate.venueWeightBps.length, expected, collector);
        checkLength("latencyPenaltyNanos", candidate.latencyPenaltyNanos.length, expected, collector);
        checkLength("toxicityPenaltyBps", candidate.toxicityPenaltyBps.length, expected, collector);
        checkLength("fillProbabilityBps", candidate.fillProbabilityBps.length, expected, collector);
        checkLength("rejectPenaltyBps", candidate.rejectPenaltyBps.length, expected, collector);
        checkLength("minChildQty", candidate.minChildQty.length, expected, collector);
        checkLength("maxChildQty", candidate.maxChildQty.length, expected, collector);
        checkLength("maxParticipationBps", candidate.maxParticipationBps.length, expected, collector);
        checkLength("routeFlags", candidate.routeFlags.length, expected, collector);

        for (int instrumentId = 0; instrumentId < candidate.instrumentCount; instrumentId++) {
            for (int regimeId = 0; regimeId < candidate.regimeCount; regimeId++) {
                for (int urgencyId = 0; urgencyId < candidate.urgencyCount; urgencyId++) {
                    boolean anyEligible = false;
                    for (int venueId = 0; venueId < candidate.venueCount; venueId++) {
                        final int idx = candidate.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        if (candidate.venueEligible[idx]) {
                            anyEligible = true;
                            lintCell(candidate, input.orderTypeCapabilities, venueId, idx, collector);
                        }
                    }
                    if (!anyEligible) {
                        collector.error(PolicyLintCode.EMPTY_ROUTE_UNIVERSE, "empty route universe for route key");
                    }
                }
            }
        }

        if (subset != null && subset.selectedVenueIds != null) {
            for (short venueId : subset.selectedVenueIds) {
                if (venueId < 0 || venueId >= candidate.venueCount) {
                    collector.error(PolicyLintCode.EMPTY_ROUTE_UNIVERSE, "strategic subset contains invalid venueId " + venueId);
                }
            }
        }
    }

    private void lintCell(
            final MutablePolicyCandidate candidate,
            final OrderTypeCapabilityMatrix capabilities,
            final int venueId,
            final int idx,
            final PolicyLintIssueCollector collector
    ) {
        checkBps("venueWeightBps", candidate.venueWeightBps[idx], PolicyLintCode.WEIGHT_OUT_OF_BOUNDS, collector);
        checkBps("toxicityPenaltyBps", candidate.toxicityPenaltyBps[idx], PolicyLintCode.PENALTY_OUT_OF_BOUNDS, collector);
        checkBps("fillProbabilityBps", candidate.fillProbabilityBps[idx], PolicyLintCode.PENALTY_OUT_OF_BOUNDS, collector);
        checkBps("rejectPenaltyBps", candidate.rejectPenaltyBps[idx], PolicyLintCode.PENALTY_OUT_OF_BOUNDS, collector);
        checkBps("maxParticipationBps", candidate.maxParticipationBps[idx], PolicyLintCode.PENALTY_OUT_OF_BOUNDS, collector);
        if (candidate.latencyPenaltyNanos[idx] < 0 || candidate.latencyPenaltyNanos[idx] > config.maxLatencyPenaltyNanos) {
            collector.error(PolicyLintCode.PENALTY_OUT_OF_BOUNDS, "latencyPenaltyNanos out of bounds");
        }
        if (candidate.minChildQty[idx] < 0 || candidate.maxChildQty[idx] <= 0 || candidate.minChildQty[idx] > candidate.maxChildQty[idx]) {
            collector.error(PolicyLintCode.CHILD_SIZE_INVALID, "child-size bounds invalid");
        }
        if (capabilities != null && !capabilities.supportsFlags(venueId, candidate.routeFlags[idx])) {
            collector.error(PolicyLintCode.UNSUPPORTED_CAPABILITY, "venue does not support requested route flags");
        }
        if (candidate.venueWeightBps[idx] == 0) {
            collector.warning(PolicyLintCode.WARNING_ONLY, "eligible venue has zero weight");
        }
    }

    private static void checkLength(
            final String name,
            final int actual,
            final int expected,
            final PolicyLintIssueCollector collector
    ) {
        if (actual != expected) {
            collector.error(PolicyLintCode.ARRAY_LENGTH_MISMATCH, name + " length mismatch");
        }
    }

    private static void checkBps(
            final String name,
            final int value,
            final String code,
            final PolicyLintIssueCollector collector
    ) {
        if (value < 0 || value > 10_000) {
            collector.error(code, name + " must be in [0,10000]");
        }
    }
}
