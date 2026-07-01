package com.nitroj.sor.api;

/**
 * Responsibility: immutable control-plane view of a parent order's latest
 * known state.
 *
 * <p>Role in system: returned by {@link SorEngine#getOrderStatus(long)} for
 * occasional OMS or operator reads. It is not intended for per-route polling.</p>
 *
 * <p>Relationships: carries {@link OrderStatusCode} and primitive quantities
 * without exposing internal `ParentOrderState`.</p>
 *
 * <p>Lifecycle: produced by an engine implementation at read time and then
 * owned by the caller.</p>
 *
 * <p>Design intent: keep status reads immutable and self-contained.</p>
 *
 * @param parentOrderId engine-assigned parent identifier
 * @param status latest lifecycle code
 * @param originalQuantity submitted parent quantity
 * @param filledQuantity cumulative filled quantity
 * @param remainingQuantity remaining quantity
 * @param updatedEpochNanos event time for the latest status update
 */
public record OrderStatus(
        long parentOrderId,
        OrderStatusCode status,
        long originalQuantity,
        long filledQuantity,
        long remainingQuantity,
        long updatedEpochNanos
) {
    /**
     * Validates the immutable order status view.
     */
    public OrderStatus {
        if (parentOrderId <= 0) {
            throw new IllegalArgumentException("parentOrderId must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (originalQuantity < 0 || filledQuantity < 0 || remainingQuantity < 0) {
            throw new IllegalArgumentException("quantities must be non-negative");
        }
        if (updatedEpochNanos < 0) {
            throw new IllegalArgumentException("updatedEpochNanos must be non-negative");
        }
    }
}
