package com.nitroj.sor.api;

/**
 * Responsibility: immutable child-order state record supplied during startup
 * hydration.
 *
 * <p>Role in system: carries venue-resting child state so recovery can rebuild
 * child orders and derive parent aggregates instead of restoring parents only.</p>
 *
 * <p>Relationships: contained by {@link OrderStateSnapshot}; each child must
 * reference a parent record from the same snapshot.</p>
 *
 * <p>Lifecycle: created by OMS/EMS, recovery, or deterministic testkit
 * hydration for one startup snapshot.</p>
 *
 * <p>Design intent: preserve conservation, duplicate-route protection, and
 * future self-liquidity guards from the first route after startup.</p>
 *
 * @param childOrderId child order identifier
 * @param parentOrderId linked parent order identifier
 * @param instrumentId dense instrument identifier
 * @param venueId dense venue identifier
 * @param side side encoded by {@link Side#BUY} or {@link Side#SELL}
 * @param limitPrice limit price in fixed-point units, or zero when absent
 * @param originalQuantity original child quantity
 * @param leavesQuantity unfilled child quantity
 * @param filledQuantity filled child quantity
 * @param status child status
 * @param updatedEpochNanos source update timestamp
 */
public record ChildOrderStateSnapshot(
        long childOrderId,
        long parentOrderId,
        int instrumentId,
        int venueId,
        int side,
        long limitPrice,
        long originalQuantity,
        long leavesQuantity,
        long filledQuantity,
        OrderStatusCode status,
        long updatedEpochNanos
) {
    /**
     * Validates child snapshot fields before they can be used for startup
     * hydration.
     */
    public ChildOrderStateSnapshot {
        if (childOrderId <= 0 || parentOrderId <= 0 || instrumentId < 0 || venueId < 0
                || !Side.isValid(side) || limitPrice < 0 || originalQuantity < 0
                || leavesQuantity < 0 || filledQuantity < 0 || status == null
                || updatedEpochNanos < 0) {
            throw new IllegalArgumentException("child order snapshot inputs must be valid");
        }
        if (filledQuantity + leavesQuantity > originalQuantity) {
            throw new IllegalArgumentException("child filled plus leaves quantity must not exceed original quantity");
        }
    }
}
