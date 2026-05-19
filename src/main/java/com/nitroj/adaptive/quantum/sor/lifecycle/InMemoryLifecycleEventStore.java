package com.nitroj.adaptive.quantum.sor.lifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Responsibility: store a bounded chronological lifecycle event stream.
 *
 * <p>Role in system: backs `/events/stream`, tests, notebooks, and console
 * logging with a small in-memory buffer.</p>
 *
 * <p>Relationships: written by lifecycle loggers and read by API/event stream
 * handlers.</p>
 *
 * <p>Lifecycle: created at startup with fixed capacity and drop policy.</p>
 *
 * <p>Design intent: high event rates are handled by bounded storage; strict mode
 * may reject appends while demo mode may drop oldest events.</p>
 */
public final class InMemoryLifecycleEventStore {
    private final int capacity;
    private final boolean dropOldestWhenFull;
    private final List<LifecycleEvent> events = new ArrayList<>();
    private int droppedCount;

    public InMemoryLifecycleEventStore(final int capacity, final boolean dropOldestWhenFull) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.dropOldestWhenFull = dropOldestWhenFull;
    }

    /**
     * Appends an event, preserving chronological snapshots by timestamp.
     *
     * @return true when stored, false when rejected by full strict buffer
     */
    public synchronized boolean append(final LifecycleEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (events.size() == capacity) {
            if (!dropOldestWhenFull) {
                droppedCount++;
                return false;
            }
            events.remove(0);
            droppedCount++;
        }
        events.add(event);
        events.sort(Comparator.comparingLong(e -> e.timestampNanos));
        return true;
    }

    /** Returns a stable chronological copy of stored events. */
    public synchronized List<LifecycleEvent> snapshot() {
        return List.copyOf(events);
    }

    public synchronized int droppedCount() {
        return droppedCount;
    }
}
