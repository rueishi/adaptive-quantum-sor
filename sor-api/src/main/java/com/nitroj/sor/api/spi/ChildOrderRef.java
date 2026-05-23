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
     * Updates all child-order fields for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
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
     */
    public ChildOrderRef clear() {
        return set(0, 0, 0, 0, 0, 0, 0);
    }

    public long childOrderId() { return childOrderId; }
    public long parentOrderId() { return parentOrderId; }
    public int venueId() { return venueId; }
    public int side() { return side; }
    public long quantity() { return quantity; }
    public long limitPrice() { return limitPrice; }
    public long createdEpochNanos() { return createdEpochNanos; }
}
