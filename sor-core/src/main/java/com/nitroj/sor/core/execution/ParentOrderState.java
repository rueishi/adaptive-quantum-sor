package com.nitroj.sor.core.execution;

/**
 * Responsibility: track routed and residual quantity for a parent order.
 *
 * <p>Role in system: reslicing uses this state to decide whether another route
 * decision is required for remaining parent quantity.</p>
 *
 * <p>Relationships: updated by {@link ResliceScheduler} from
 * {@link RouteDecisionResult} values.</p>
 *
 * <p>Lifecycle: created when a parent order enters the system and retained
 * until the order is complete.</p>
 *
 * <p>Design intent: keep parent lifecycle accounting explicit and deterministic
 * without introducing API order views yet.</p>
 */
public final class ParentOrderState {
    public final long parentOrderId;
    public final long totalQty;
    private long routedQty;
    private long residualQty;

    public ParentOrderState(final long parentOrderId, final long totalQty) {
        if (parentOrderId <= 0 || totalQty <= 0) {
            throw new IllegalArgumentException("parentOrderId and totalQty must be positive");
        }
        this.parentOrderId = parentOrderId;
        this.totalQty = totalQty;
        this.residualQty = totalQty;
    }

    /** Applies a route decision to parent lifecycle quantities. */
    public void apply(final RouteDecisionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        routedQty += result.routedQty;
        residualQty = Math.max(0L, totalQty - routedQty);
    }

    public long routedQty() {
        return routedQty;
    }

    public long residualQty() {
        return residualQty;
    }
}
