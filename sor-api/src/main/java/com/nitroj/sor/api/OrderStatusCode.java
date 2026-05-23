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
    ACCEPTED,
    ROUTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    UNKNOWN
}
