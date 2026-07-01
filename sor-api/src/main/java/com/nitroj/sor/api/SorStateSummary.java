package com.nitroj.sor.api;

/**
 * Responsibility: immutable control-plane summary of engine runtime state.
 *
 * <p>Role in system: exposes counts, readiness phase, reset evidence, and
 * hydration evidence without returning mutable internal order-book
 * structures.</p>
 *
 * <p>Relationships: returned by {@link SorControlPlane#stateSummary()}.</p>
 *
 * <p>Lifecycle: produced on demand and owned by the caller.</p>
 *
 * <p>Design intent: make diagnostics safe for notebooks, tests, and remote
 * control surfaces.</p>
 *
 * @param activeParentOrderCount number of active parent orders
 * @param activeChildOrderCount number of active child orders
 * @param pendingChildQuantity aggregate pending child quantity
 * @param activePolicyVersion currently active policy version
 * @param lastResetSummary most recent reset summary, if any
 * @param readinessPhase current readiness phase
 * @param lastHydrationSummary most recent startup hydration summary, if any
 */
public record SorStateSummary(
        int activeParentOrderCount,
        int activeChildOrderCount,
        long pendingChildQuantity,
        long activePolicyVersion,
        SorResetSummary lastResetSummary,
        SorReadinessPhase readinessPhase,
        SorStartupHydrationSummary lastHydrationSummary
) {
    /**
     * Backward-compatible constructor for older callers that do not yet expose
     * readiness phase or hydration evidence.
     */
    public SorStateSummary(final int activeParentOrderCount,
                           final int activeChildOrderCount,
                           final long pendingChildQuantity,
                           final long activePolicyVersion,
                           final SorResetSummary lastResetSummary) {
        this(activeParentOrderCount, activeChildOrderCount, pendingChildQuantity,
                activePolicyVersion, lastResetSummary, SorReadinessPhase.CONSTRUCTED, null);
    }

    /**
     * Validates non-negative state counters.
     */
    public SorStateSummary {
        if (activeParentOrderCount < 0 || activeChildOrderCount < 0 || pendingChildQuantity < 0
                || activePolicyVersion < 0 || readinessPhase == null) {
            throw new IllegalArgumentException("state summary counts must be non-negative");
        }
    }
}
