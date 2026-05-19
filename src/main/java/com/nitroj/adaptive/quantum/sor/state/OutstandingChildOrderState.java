package com.nitroj.adaptive.quantum.sor.state;

/**
 * Responsibility: track outstanding child order quantities by fixed slot.
 *
 * <p>Role in system: this near-hot-path state lets execution and venue
 * simulation coordinate outstanding quantity without a database or map lookup.</p>
 *
 * <p>Relationships: child orders populate slots here, and venue behavior later
 * reduces remaining quantity as fills arrive.</p>
 *
 * <p>Lifecycle: created with fixed capacity during startup and reused for the
 * process lifetime.</p>
 *
 * <p>Design intent: dense slots are sufficient until real exchange order IDs are
 * introduced by later integrations.</p>
 */
public final class OutstandingChildOrderState {
    private final long[] childOrderIds;
    private final long[] remainingQty;

    public OutstandingChildOrderState(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.childOrderIds = new long[capacity];
        this.remainingQty = new long[capacity];
    }

    /**
     * Records a child order in a fixed slot.
     *
     * @param slot dense state slot
     * @param childOrderId child order identifier
     * @param quantity outstanding quantity
     */
    public void put(final int slot, final long childOrderId, final long quantity) {
        checkSlot(slot);
        if (childOrderId <= 0 || quantity < 0) {
            throw new IllegalArgumentException("childOrderId must be positive and quantity non-negative");
        }
        childOrderIds[slot] = childOrderId;
        remainingQty[slot] = quantity;
    }

    /**
     * Applies a fill quantity to a slot.
     *
     * @param slot dense state slot
     * @param fillQty fill quantity to subtract
     */
    public void applyFill(final int slot, final long fillQty) {
        checkSlot(slot);
        if (fillQty < 0) {
            throw new IllegalArgumentException("fillQty must be non-negative");
        }
        remainingQty[slot] = Math.max(0L, remainingQty[slot] - fillQty);
    }

    public long childOrderId(final int slot) {
        checkSlot(slot);
        return childOrderIds[slot];
    }

    public long remainingQty(final int slot) {
        checkSlot(slot);
        return remainingQty[slot];
    }

    private void checkSlot(final int slot) {
        if (slot < 0 || slot >= childOrderIds.length) {
            throw new IndexOutOfBoundsException("slot out of range: " + slot);
        }
    }
}
