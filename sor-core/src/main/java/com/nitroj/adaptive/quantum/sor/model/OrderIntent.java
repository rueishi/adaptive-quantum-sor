package com.nitroj.adaptive.quantum.sor.model;

/**
 * Responsibility: represent a parent order request entering the SOR.
 *
 * <p>Role in system: this is the hot-path input contract consumed by the future
 * policy-driven executioner. It stores only primitive routing fields required
 * by early task cards.</p>
 *
 * <p>Relationships: {@link ParentOrderIntentQueue} stores instances of this
 * class; future execution and API layers will read the same fields to create
 * child orders and status views.</p>
 *
 * <p>Lifecycle: order intents are created by simulators or API code, enqueued,
 * then consumed by routing. They are immutable after construction to preserve
 * deterministic routing input.</p>
 *
 * <p>Design intent: explicit primitive fields keep parent-order validation close
 * to construction and avoid relying on maps or JSON objects in the hot path.</p>
 */
public final class OrderIntent {
    public final long parentOrderId;
    public final int instrumentId;
    public final int side;
    public final long quantity;
    public final int urgencyId;
    public final long createdAtNanos;

    /**
     * Creates a validated parent order intent.
     *
     * @param parentOrderId dense or generated parent order ID
     * @param instrumentId dense instrument ID
     * @param side {@link Side#BUY} or {@link Side#SELL}
     * @param quantity positive parent quantity
     * @param urgencyId dense urgency ID
     * @param createdAtNanos creation timestamp
     */
    public OrderIntent(
            final long parentOrderId,
            final int instrumentId,
            final int side,
            final long quantity,
            final int urgencyId,
            final long createdAtNanos
    ) {
        if (parentOrderId <= 0) {
            throw new IllegalArgumentException("parentOrderId must be positive");
        }
        if (instrumentId < 0) {
            throw new IllegalArgumentException("instrumentId must be non-negative");
        }
        if (!Side.isValid(side)) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (urgencyId < 0) {
            throw new IllegalArgumentException("urgencyId must be non-negative");
        }
        this.parentOrderId = parentOrderId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
        this.urgencyId = urgencyId;
        this.createdAtNanos = createdAtNanos;
    }
}
