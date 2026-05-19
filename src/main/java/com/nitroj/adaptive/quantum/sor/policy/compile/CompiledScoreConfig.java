package com.nitroj.adaptive.quantum.sor.policy.compile;

/**
 * Responsibility: configure deterministic Phase 1 policy compilation limits.
 *
 * <p>Role in system: the compiler uses this config to cap route-list length
 * and assign policy identity metadata.</p>
 *
 * <p>Relationships: consumed by {@link DefaultPolicyCompiler}.</p>
 *
 * <p>Lifecycle: immutable after construction and reused across compile attempts.</p>
 *
 * <p>Design intent: keep compiler behavior deterministic without introducing a
 * broader policy language or scoring framework in this task card.</p>
 */
public final class CompiledScoreConfig {
    public final int maxEligibleVenuesPerRoute;
    public final int optimizerType;
    public final int policyState;

    public CompiledScoreConfig(final int maxEligibleVenuesPerRoute, final int optimizerType, final int policyState) {
        if (maxEligibleVenuesPerRoute <= 0) {
            throw new IllegalArgumentException("maxEligibleVenuesPerRoute must be positive");
        }
        this.maxEligibleVenuesPerRoute = maxEligibleVenuesPerRoute;
        this.optimizerType = optimizerType;
        this.policyState = policyState;
    }

    public static CompiledScoreConfig defaults() {
        return new CompiledScoreConfig(4, 1, 1);
    }
}
