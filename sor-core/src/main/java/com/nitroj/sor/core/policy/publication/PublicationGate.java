package com.nitroj.sor.core.policy.publication;

import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;

/**
 * Responsibility: enforce Phase 1 policy publication gates.
 *
 * <p>Role in system: this gate prevents invalid, insufficiently improved, too
 * frequent, or excessive-churn policies from becoming active.</p>
 *
 * <p>Relationships: consumed by {@link com.nitroj.sor.core.policy.PolicyPublisher}
 * and reads lint/validation reports plus {@link PolicyDiff} churn summaries.</p>
 *
 * <p>Lifecycle: stateless except immutable thresholds configured at startup.</p>
 *
 * <p>Design intent: keep publication safety separate from the atomic reference
 * swap so gate decisions are easy to test.</p>
 */
public final class PublicationGate {
    private final long minPublishIntervalNanos;
    private final int minExpectedImprovementBps;
    private final int maxVenueChanges;
    private final int maxWeightChangeBps;

    public PublicationGate(
            final long minPublishIntervalNanos,
            final int minExpectedImprovementBps,
            final int maxVenueChanges,
            final int maxWeightChangeBps
    ) {
        if (minPublishIntervalNanos < 0 || minExpectedImprovementBps < 0 || maxVenueChanges < 0 || maxWeightChangeBps < 0) {
            throw new IllegalArgumentException("publication gate thresholds must be non-negative");
        }
        this.minPublishIntervalNanos = minPublishIntervalNanos;
        this.minExpectedImprovementBps = minExpectedImprovementBps;
        this.maxVenueChanges = maxVenueChanges;
        this.maxWeightChangeBps = maxWeightChangeBps;
    }

    public static PublicationGate permissive() {
        return new PublicationGate(0L, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Evaluates all configured publication gates.
     */
    public PublicationGateResult evaluate(
            final SorPolicy activePolicy,
            final SorPolicy candidate,
            final PolicyValidationReport validationReport,
            final PolicyLintReport lintReport,
            final PolicyDiff diff,
            final long nowNanos,
            final long lastPublishNanos,
            final int expectedImprovementBps
    ) {
        if (candidate == null) {
            return PublicationGateResult.rejected("candidate_missing");
        }
        if (validationReport == null || !validationReport.valid()) {
            return PublicationGateResult.rejected("validation_failed");
        }
        if (lintReport != null && lintReport.hasErrors()) {
            return PublicationGateResult.rejected("lint_failed");
        }
        if (candidate.policyHash64 == 0L || candidate.policyHashSha256 == null || candidate.policyHashSha256.length != 32) {
            return PublicationGateResult.rejected("hash_mismatch");
        }
        if (activePolicy != null && nowNanos - lastPublishNanos < minPublishIntervalNanos) {
            return PublicationGateResult.rejected("publish_interval");
        }
        if (expectedImprovementBps < minExpectedImprovementBps) {
            return PublicationGateResult.rejected("expected_improvement");
        }
        if (diff != null && diff.addedVenueCount > maxVenueChanges) {
            return PublicationGateResult.rejected("venue_churn");
        }
        if (diff != null && diff.maxWeightChangeBps > maxWeightChangeBps) {
            return PublicationGateResult.rejected("weight_churn");
        }
        return PublicationGateResult.allowed(expectedImprovementBps, diff == null ? 0 : diff.addedVenueCount, diff == null ? 0 : diff.maxWeightChangeBps);
    }
}
