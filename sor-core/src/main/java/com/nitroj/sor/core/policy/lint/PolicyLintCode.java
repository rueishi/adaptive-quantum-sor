package com.nitroj.sor.core.policy.lint;

/**
 * Responsibility: define stable lint issue codes.
 *
 * <p>Role in system: validators, tests, and operator views can reason about
 * lint failures without parsing free-form messages.</p>
 *
 * <p>Relationships: emitted by {@link DefaultPolicyLint} through
 * {@link PolicyLintIssueCollector}.</p>
 *
 * <p>Lifecycle: codes remain stable once published by a task card.</p>
 *
 * <p>Design intent: string constants keep report serialization simple in Phase 1.</p>
 */
public final class PolicyLintCode {
    public static final String ARRAY_LENGTH_MISMATCH = "ARRAY_LENGTH_MISMATCH";
    public static final String EMPTY_ROUTE_UNIVERSE = "EMPTY_ROUTE_UNIVERSE";
    public static final String UNSUPPORTED_CAPABILITY = "UNSUPPORTED_CAPABILITY";
    public static final String WEIGHT_OUT_OF_BOUNDS = "WEIGHT_OUT_OF_BOUNDS";
    public static final String PENALTY_OUT_OF_BOUNDS = "PENALTY_OUT_OF_BOUNDS";
    public static final String CHILD_SIZE_INVALID = "CHILD_SIZE_INVALID";
    public static final String WARNING_ONLY = "WARNING_ONLY";
    public static final String LINT_EXCEPTION = "LINT_EXCEPTION";

    private PolicyLintCode() {
    }
}
