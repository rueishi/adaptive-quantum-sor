package com.nitroj.sor.testserver;

import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEvent;

/**
 * Responsibility: render lifecycle events as server-sent-event text.
 *
 * <p>Role in system: backs the notebook/demo {@code GET /events/stream} and
 * {@code GET /scenario/events} endpoints exposed by {@link NotebookScenarioHttpServer}.
 * It turns the in-memory lifecycle event store into a simple SSE-compatible
 * text stream that can be consumed by notebooks, command-line HTTP clients, and
 * lightweight browser tooling.</p>
 *
 * <p>Relationships: reads immutable snapshots from
 * {@link InMemoryLifecycleEventStore} and serializes {@link LifecycleEvent}
 * values. It does not mutate engine, scenario, or lifecycle state.</p>
 *
 * <p>Lifecycle: constructed with the event store owned by the surrounding test
 * server and used per HTTP stream response. The helper has no background
 * thread, socket, or resource lifecycle of its own.</p>
 *
 * <p>Design intent: keep the test-server event stream dependency-free. The
 * JDK HTTP server can return this rendered string directly, avoiding WebSocket
 * or reactive-stream dependencies for notebook/demo visibility.</p>
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
