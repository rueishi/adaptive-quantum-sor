package com.nitroj.sor.core.audit;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: store fixed-size compact route audit events.
 *
 * <p>Role in system: provides Phase 1 in-memory audit retention before durable
 * audit persistence exists.</p>
 *
 * <p>Relationships: written by {@link InMemoryRouteAuditWriter} and read by
 * tests or future audit APIs.</p>
 *
 * <p>Lifecycle: allocated with fixed capacity at startup.</p>
 *
 * <p>Design intent: bounded storage makes full-buffer behavior deterministic.</p>
 */
public final class RouteAuditEventBuffer {
    private final int capacity;
    private final boolean dropOldestWhenFull;
    private final List<RouteAuditEvent> events = new ArrayList<>();
    private int droppedCount;

    public RouteAuditEventBuffer(final int capacity, final boolean dropOldestWhenFull) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.dropOldestWhenFull = dropOldestWhenFull;
    }

    /** Appends an event according to configured full-buffer behavior. */
    public synchronized boolean append(final RouteAuditEvent event) {
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
        return true;
    }

    public synchronized List<RouteAuditEvent> snapshot() {
        return List.copyOf(events);
    }

    public synchronized int droppedCount() {
        return droppedCount;
    }
}
