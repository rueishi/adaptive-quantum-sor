package com.nitroj.adaptive.quantum.sor.policy.validation;

import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

/**
 * Responsibility: define compiled policy artifact validation.
 *
 * <p>Role in system: validators check immutable policy structures before
 * publication gates can atomically swap active policy references.</p>
 *
 * <p>Relationships: implemented by {@link DefaultPolicyValidator} and consumed
 * by publication gate/publisher flows.</p>
 *
 * <p>Lifecycle: called after compilation, before publication.</p>
 *
 * <p>Design intent: validation returns a report rather than mutating policy or
 * throwing for expected artifact defects.</p>
 */
public interface PolicyValidator {
    /**
     * Validates a compiled policy candidate.
     */
    PolicyValidationReport validate(SorPolicy policy);
}
