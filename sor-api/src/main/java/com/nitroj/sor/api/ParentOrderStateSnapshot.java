package com.nitroj.sor.api;

/**
 * Responsibility: immutable parent-order state record supplied during startup
 * hydration.
 *
 * <p>Role in system: represents OMS/EMS authoritative parent working state
 * without pretending recovered state arrived through
 * {@link SorEngine#submitParentOrder(ParentOrderRequest)}.</p>
 *
 * <p>Relationships: contained by {@link OrderStateSnapshot} and cross-checked
 * against {@link ChildOrderStateSnapshot} records when the engine performs the
 * warm-path bulk apply owned by later Phase 9 cards.</p>
 *
 * <p>Lifecycle: created by OMS/EMS, recovery, or testkit startup code for one
 * snapshot and owned by the control-plane caller.</p>
 *
 * <p>Design intent: keep startup state explicit, primitive, and independent of
 * engine implementation classes.</p>
 *
 * @param parentOrderId engine or OMS/EMS parent order identifier
 * @param instrumentId dense instrument identifier
 * @param side side encoded by {@link Side#BUY} or {@link Side#SELL}
 * @param limitPrice limit price in fixed-point units, or zero when absent
 * @param originalQuantity original parent quantity
 * @param leavesQuantity unfilled parent quantity
 * @param filledQuantity filled parent quantity
 * @param pendingChildQuantity quantity currently resting in child orders
 * @param status parent status
 * @param updatedEpochNanos source update timestamp
 */
public record ParentOrderStateSnapshot(
        long parentOrderId,
        int instrumentId,
        int side,
        long limitPrice,
        long originalQuantity,
        long leavesQuantity,
        long filledQuantity,
        long pendingChildQuantity,
        OrderStatusCode status,
        long updatedEpochNanos
) {
    /**
     * Validates parent snapshot fields before they can be used for startup
     * hydration.
     */
    public ParentOrderStateSnapshot {
        if (parentOrderId <= 0 || instrumentId < 0 || !Side.isValid(side) || limitPrice < 0
                || originalQuantity < 0 || leavesQuantity < 0 || filledQuantity < 0
                || pendingChildQuantity < 0 || status == null || updatedEpochNanos < 0) {
            throw new IllegalArgumentException("parent order snapshot inputs must be valid");
        }
        if (filledQuantity + leavesQuantity > originalQuantity) {
            throw new IllegalArgumentException("parent filled plus leaves quantity must not exceed original quantity");
        }
    }
}
