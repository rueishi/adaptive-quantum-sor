package com.nitroj.adaptive.quantum.sor.model;

/**
 * Responsibility: define primitive order lifecycle status codes.
 *
 * <p>Role in system: parent and child order state containers use these codes to
 * represent lifecycle without allocating status objects. The codes are broad
 * enough for Phase 1 simulators and routing tests.</p>
 *
 * <p>Relationships: {@code ParentOrderIntentQueue}, {@code ChildOrderState},
 * and future API views can expose or store these stable status values.</p>
 *
 * <p>Lifecycle: static constants are immutable and process-wide.</p>
 *
 * <p>Design intent: primitive status codes keep hot and near-hot structures
 * compact while remaining readable through named constants.</p>
 */
public final class OrderStatus {
    public static final int NEW = 0;
    public static final int ACKED = 1;
    public static final int PARTIALLY_FILLED = 2;
    public static final int FILLED = 3;
    public static final int REJECTED = 4;
    public static final int CANCELLED = 5;
    public static final int NO_ACTIVE_POLICY = 6;
    public static final int NO_LIQUIDITY = 7;

    private OrderStatus() {
    }
}
