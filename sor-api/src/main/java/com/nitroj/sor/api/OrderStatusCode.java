package com.nitroj.sor.api;

/**
 * Responsibility: public lifecycle codes for a parent order.
 *
 * <p>Role in system: appears inside {@link OrderStatus} so control-plane status
 * reads do not depend on internal execution-state classes.</p>
 *
 * <p>Relationships: returned from {@link SorEngine#getOrderStatus(long)}.</p>
 *
 * <p>Lifecycle: values may grow additively in future API versions.</p>
 *
 * <p>Design intent: provide a compact status vocabulary suitable for Java and
 * future wire-codec clients.</p>
 */
public enum OrderStatusCode {
    /** Parent order has been accepted by the engine. */
    ACCEPTED,
    /** Parent order has produced one or more child routes. */
    ROUTED,
    /** Parent order has some filled quantity and remaining open quantity. */
    PARTIALLY_FILLED,
    /** Parent order is fully filled. */
    FILLED,
    /** Parent order has been cancelled. */
    CANCELLED,
    /** Parent order or its routed child flow has been rejected. */
    REJECTED,
    /** Parent order is not known to the engine. */
    UNKNOWN
}
