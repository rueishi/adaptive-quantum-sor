package com.nitroj.adaptive.quantum.sor.model;

/**
 * Responsibility: provide a fixed-capacity FIFO for parent order intents.
 *
 * <p>Role in system: this queue is the Phase 1 boundary between simulated/API
 * parent order submission and the future SOR execution loop.</p>
 *
 * <p>Relationships: simulators and API handlers enqueue {@link OrderIntent}
 * instances; execution code polls them. This class intentionally does not own
 * routing or validation beyond queue capacity.</p>
 *
 * <p>Lifecycle: created during engine startup with a fixed capacity, then reused
 * for the process lifetime.</p>
 *
 * <p>Design intent: the ring buffer shape mirrors production hot-path queues
 * without introducing external concurrency libraries in the scaffold.</p>
 */
public final class ParentOrderIntentQueue {
    private final OrderIntent[] entries;
    private int head;
    private int tail;
    private int size;

    /**
     * Creates a fixed-capacity intent queue.
     *
     * @param capacity maximum number of queued parent intents
     */
    public ParentOrderIntentQueue(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.entries = new OrderIntent[capacity];
    }

    /**
     * Attempts to enqueue a parent order.
     *
     * @param intent validated parent order intent
     * @return true if queued, false if the queue is full
     */
    public boolean offer(final OrderIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("intent must not be null");
        }
        if (size == entries.length) {
            return false;
        }
        entries[tail] = intent;
        tail = (tail + 1) % entries.length;
        size++;
        return true;
    }

    /**
     * Removes and returns the next parent order if present.
     *
     * @return next intent or null when the queue is empty
     */
    public OrderIntent poll() {
        if (size == 0) {
            return null;
        }
        final OrderIntent intent = entries[head];
        entries[head] = null;
        head = (head + 1) % entries.length;
        size--;
        return intent;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return entries.length;
    }
}
