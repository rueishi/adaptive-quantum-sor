package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable child-order reference offered to a venue
 * ring.
 *
 * <p>Role in system: the engine populates this carrier and passes it to
 * {@link RingWriter#offer(ChildOrderRef)}.</p>
 *
 * <p>Relationships: consumed by {@link VenueAdapter} implementations.</p>
 *
 * <p>Lifecycle: owned by the engine on the hot path and copied by ring writers
 * as needed.</p>
 *
 * <p>Design intent: primitive fields avoid allocation and implementation type
 * leakage.</p>
 */
public final class ChildOrderRef {
    private long childOrderId;
    private long parentOrderId;
    private int venueId;
    private int side;
    private long quantity;
    private long limitPrice;
    private long createdEpochNanos;

    /**
     * Creates an empty reusable child-order reference.
     */
    public ChildOrderRef() {
    }

    /**
     * Updates all child-order fields for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param childOrderId engine-assigned child order identifier
     * @param parentOrderId engine-assigned parent order identifier
     * @param venueId destination venue identifier
     * @param side side encoded by {@code Side.BUY} or {@code Side.SELL}
     * @param quantity child order quantity
     * @param limitPrice limit price in fixed-point price units
     * @param createdEpochNanos wall-clock creation timestamp in epoch nanoseconds
     * @return this reusable reference
     */
    public ChildOrderRef set(final long childOrderId, final long parentOrderId, final int venueId,
                             final int side, final long quantity, final long limitPrice,
                             final long createdEpochNanos) {
        this.childOrderId = childOrderId;
        this.parentOrderId = parentOrderId;
        this.venueId = venueId;
        this.side = side;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.createdEpochNanos = createdEpochNanos;
        return this;
    }

    /**
     * Clears all fields to zero.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return this reusable reference
     */
    public ChildOrderRef clear() {
        return set(0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Returns engine-assigned child order identifier.
     *
     * @return engine-assigned child order identifier
     */
    public long childOrderId() { return childOrderId; }
    /**
     * Returns engine-assigned parent order identifier.
     *
     * @return engine-assigned parent order identifier
     */
    public long parentOrderId() { return parentOrderId; }
    /**
     * Returns destination venue identifier.
     *
     * @return destination venue identifier
     */
    public int venueId() { return venueId; }
    /**
     * Returns side encoded by {@code Side.BUY} or {@code Side.SELL}.
     *
     * @return side encoded by {@code Side.BUY} or {@code Side.SELL}
     */
    public int side() { return side; }
    /**
     * Returns child order quantity.
     *
     * @return child order quantity
     */
    public long quantity() { return quantity; }
    /**
     * Returns limit price in fixed-point price units.
     *
     * @return limit price in fixed-point price units
     */
    public long limitPrice() { return limitPrice; }
    /**
     * Returns wall-clock creation timestamp in epoch nanoseconds.
     *
     * @return wall-clock creation timestamp in epoch nanoseconds
     */
    public long createdEpochNanos() { return createdEpochNanos; }
}
