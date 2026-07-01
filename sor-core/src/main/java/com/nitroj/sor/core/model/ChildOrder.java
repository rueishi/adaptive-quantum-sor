package com.nitroj.sor.core.model;

/**
 * Responsibility: store a routed child order record.
 *
 * <p>Role in system: child orders are produced by the policy-driven or static
 * SOR executioners and consumed by venue simulation, audit, and status views.</p>
 *
 * <p>Relationships: {@link ChildOrderBuffer} preallocates and reuses instances
 * of this class. Future venue simulators update child lifecycle state using the
 * identifiers stored here.</p>
 *
 * <p>Lifecycle: instances are created when the buffer is constructed and then
 * repeatedly populated and cleared as route decisions occur.</p>
 *
 * <p>Design intent: mutable primitive fields allow the execution path to reuse
 * records without allocation after warmup.</p>
 */
public final class ChildOrder {
    public long childOrderId;
    public long parentOrderId;
    public int instrumentId;
    public int venueId;
    public int side;
    public long quantity;
    public long policyVersion;
    public long policyHash64;
    public int status;

    /**
     * Populates this preallocated child-order slot.
     *
     * @param childOrderId generated child order ID
     * @param parentOrderId parent order ID
     * @param instrumentId dense instrument ID
     * @param venueId dense venue ID
     * @param side order side
     * @param quantity positive child quantity
     * @param policyVersion policy version used for route decision
     * @param policyHash64 policy hash used for route decision
     */
    public void set(
            final long childOrderId,
            final long parentOrderId,
            final int instrumentId,
            final int venueId,
            final int side,
            final long quantity,
            final long policyVersion,
            final long policyHash64
    ) {
        if (childOrderId <= 0 || parentOrderId <= 0) {
            throw new IllegalArgumentException("order IDs must be positive");
        }
        if (instrumentId < 0 || venueId < 0) {
            throw new IllegalArgumentException("instrumentId and venueId must be non-negative");
        }
        if (!Side.isValid(side)) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.childOrderId = childOrderId;
        this.parentOrderId = parentOrderId;
        this.instrumentId = instrumentId;
        this.venueId = venueId;
        this.side = side;
        this.quantity = quantity;
        this.policyVersion = policyVersion;
        this.policyHash64 = policyHash64;
        this.status = OrderStatus.NEW;
    }

    /**
     * Clears this slot before reuse.
     */
    public void clear() {
        childOrderId = 0L;
        parentOrderId = 0L;
        instrumentId = 0;
        venueId = 0;
        side = 0;
        quantity = 0L;
        policyVersion = 0L;
        policyHash64 = 0L;
        status = OrderStatus.NEW;
    }
}
