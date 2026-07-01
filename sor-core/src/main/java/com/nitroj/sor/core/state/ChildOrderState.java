package com.nitroj.sor.core.state;

/**
 * Responsibility: store child order lifecycle status by fixed slot.
 *
 * <p>Role in system: venue simulation updates this state as ACK, fill, reject,
 * or cancel events occur. API and feature aggregation code can then read the
 * latest compact status.</p>
 *
 * <p>Relationships: complements {@link OutstandingChildOrderState}; one stores
 * lifecycle status while the other stores remaining quantity.</p>
 *
 * <p>Lifecycle: allocated once with fixed capacity and updated in place.</p>
 *
 * <p>Design intent: primitive arrays are predictable for Phase 1 tests and later
 * hot-path checks.</p>
 */
public final class ChildOrderState {
    private final int[] statusBySlot;
    private final long[] filledQtyBySlot;

    public ChildOrderState(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.statusBySlot = new int[capacity];
        this.filledQtyBySlot = new long[capacity];
    }

    /**
     * Updates status and cumulative filled quantity for a child slot.
     *
     * @param slot dense child slot
     * @param status primitive order status code
     * @param filledQty cumulative filled quantity
     */
    public void update(final int slot, final int status, final long filledQty) {
        checkSlot(slot);
        if (filledQty < 0) {
            throw new IllegalArgumentException("filledQty must be non-negative");
        }
        statusBySlot[slot] = status;
        filledQtyBySlot[slot] = filledQty;
    }

    public int status(final int slot) {
        checkSlot(slot);
        return statusBySlot[slot];
    }

    public long filledQty(final int slot) {
        checkSlot(slot);
        return filledQtyBySlot[slot];
    }

    private void checkSlot(final int slot) {
        if (slot < 0 || slot >= statusBySlot.length) {
            throw new IndexOutOfBoundsException("slot out of range: " + slot);
        }
    }
}
