package com.nitroj.sor.core.model;

/**
 * Responsibility: maintain fixed-capacity child orders for one route decision.
 *
 * <p>Role in system: executioners write child orders into this buffer instead
 * of allocating per routed venue. The buffer is then handed to venue simulation
 * and audit components.</p>
 *
 * <p>Relationships: owns preallocated {@link ChildOrder} instances and exposes
 * them by index for deterministic tests and later execution loops.</p>
 *
 * <p>Lifecycle: created during startup or benchmark fixture setup, reset before
 * each route decision, and reused.</p>
 *
 * <p>Design intent: fixed capacity and explicit failure on overflow keep Phase 1
 * behavior deterministic and easy to benchmark.</p>
 */
public final class ChildOrderBuffer {
    private final ChildOrder[] orders;
    private int size;

    /**
     * Creates a preallocated child order buffer.
     *
     * @param capacity maximum child orders per route decision
     */
    public ChildOrderBuffer(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.orders = new ChildOrder[capacity];
        for (int i = 0; i < capacity; i++) {
            orders[i] = new ChildOrder();
        }
    }

    /**
     * Adds and populates a child order.
     *
     * @return populated child order slot
     */
    public ChildOrder add(
            final long childOrderId,
            final long parentOrderId,
            final int instrumentId,
            final int venueId,
            final int side,
            final long quantity,
            final long policyVersion,
            final long policyHash64
    ) {
        if (size == orders.length) {
            throw new IllegalStateException("child order buffer is full");
        }
        final ChildOrder order = orders[size++];
        order.set(childOrderId, parentOrderId, instrumentId, venueId, side, quantity, policyVersion, policyHash64);
        return order;
    }

    /**
     * Clears active entries while preserving preallocated slots.
     */
    public void reset() {
        for (int i = 0; i < size; i++) {
            orders[i].clear();
        }
        size = 0;
    }

    /**
     * Returns an active child order by index.
     *
     * @param index active index in {@code [0, size)}
     * @return child order slot
     */
    public ChildOrder get(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("child order index out of range: " + index);
        }
        return orders[index];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return orders.length;
    }
}
