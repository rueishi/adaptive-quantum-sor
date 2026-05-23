package com.nitroj.adaptive.quantum.sor.lifecycle;

/**
 * Responsibility: carry one compact lifecycle event.
 *
 * <p>Role in system: the narrative logger turns these primitive fields into a
 * human-readable chronological story for notebooks, HTTP streaming, and tests.</p>
 *
 * <p>Relationships: stored by {@link InMemoryLifecycleEventStore} and published
 * through {@link NarrativeLifecycleLogger}.</p>
 *
 * <p>Lifecycle: created by warm/control-plane components and retained in a
 * bounded store for observation.</p>
 *
 * <p>Design intent: keeps lifecycle messages separate from hot-path audit
 * records while still allowing a readable story.</p>
 */
public final class LifecycleEvent {
    public long eventId;
    public long timestampNanos;
    public int componentId;
    public int eventType;
    public long correlationId;
    public long policyVersion;
    public long parentOrderId;
    public long childOrderId;
    public String message;

    /**
     * Creates a lifecycle event with a required message and timestamp.
     */
    public LifecycleEvent(
            final long eventId,
            final long timestampNanos,
            final int componentId,
            final int eventType,
            final long correlationId,
            final String message
    ) {
        if (eventId <= 0 || timestampNanos < 0) {
            throw new IllegalArgumentException("eventId must be positive and timestampNanos non-negative");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        this.eventId = eventId;
        this.timestampNanos = timestampNanos;
        this.componentId = componentId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.message = message;
    }
}
