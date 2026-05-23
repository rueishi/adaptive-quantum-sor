package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;

/**
 * Responsibility: render lifecycle events as server-sent-event text.
 *
 * <p>Role in system: backs GET /events/stream for notebooks.</p>
 *
 * <p>Relationships: reads {@link InMemoryLifecycleEventStore} snapshots.</p>
 *
 * <p>Lifecycle: stateless helper used per HTTP stream response.</p>
 *
 * <p>Design intent: SSE text is enough for Phase 1 without a WebSocket stack.</p>
 */
public final class EventStreamHandler {
    private final InMemoryLifecycleEventStore store;

    public EventStreamHandler(final InMemoryLifecycleEventStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
    }

    /** Renders stored lifecycle events in SSE format. */
    public String render() {
        final StringBuilder builder = new StringBuilder();
        for (LifecycleEvent event : store.snapshot()) {
            builder.append("id: ").append(event.eventId).append("\n");
            builder.append("event: lifecycle\n");
            builder.append("timestampNanos: ").append(event.timestampNanos).append("\n");
            builder.append("componentId: ").append(event.componentId).append("\n");
            builder.append("eventType: ").append(event.eventType).append("\n");
            builder.append("correlationId: ").append(event.correlationId).append("\n");
            builder.append("data: ").append(event.message).append("\n\n");
        }
        return builder.toString();
    }
}
