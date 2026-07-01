package com.nitroj.sor.core.policy.lint;

import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;

/**
 * Responsibility: define the policy candidate lint contract.
 *
 * <p>Role in system: lint is the first publication gate for mutable optimizer
 * output before compilation creates immutable policy artifacts.</p>
 *
 * <p>Relationships: implemented by {@link DefaultPolicyLint} and consumed by
 * compiler/publisher orchestration.</p>
 *
 * <p>Lifecycle: called during warm-path policy optimization, never by route
 * execution.</p>
 *
 * <p>Design intent: return reports rather than throwing for expected candidate
 * defects so callers can preserve the active policy safely.</p>
 */
public interface PolicyLint {
    /**
     * Lints a mutable candidate against optimizer input and strategic subset.
     */
    PolicyLintReport lint(MutablePolicyCandidate candidate, PolicyOptimizationInput input, StrategicVenueSubsetResult subset);
}
