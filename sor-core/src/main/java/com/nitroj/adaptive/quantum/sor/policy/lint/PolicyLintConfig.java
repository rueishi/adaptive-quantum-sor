package com.nitroj.adaptive.quantum.sor.policy.lint;

/**
 * Responsibility: configure Phase 1 policy lint bounds.
 *
 * <p>Role in system: lint checks use this immutable config to decide whether
 * warning-only candidates are allowed and which value bounds are valid.</p>
 *
 * <p>Relationships: passed to {@link DefaultPolicyLint} by tests and later
 * optimizer/policy coordination.</p>
 *
 * <p>Lifecycle: created at startup or test setup and reused for lint runs.</p>
 *
 * <p>Design intent: expose only the minimal MVP controls owned by P1-TC-012.</p>
 */
public final class PolicyLintConfig {
    public final boolean allowWarnings;
    public final int maxPenaltyBps;
    public final int maxLatencyPenaltyNanos;

    public PolicyLintConfig(final boolean allowWarnings, final int maxPenaltyBps, final int maxLatencyPenaltyNanos) {
        if (maxPenaltyBps < 0 || maxPenaltyBps > 10_000) {
            throw new IllegalArgumentException("maxPenaltyBps must be in [0,10000]");
        }
        if (maxLatencyPenaltyNanos < 0) {
            throw new IllegalArgumentException("maxLatencyPenaltyNanos must be non-negative");
        }
        this.allowWarnings = allowWarnings;
        this.maxPenaltyBps = maxPenaltyBps;
        this.maxLatencyPenaltyNanos = maxLatencyPenaltyNanos;
    }

    public static PolicyLintConfig defaults() {
        return new PolicyLintConfig(true, 10_000, Integer.MAX_VALUE);
    }
}
