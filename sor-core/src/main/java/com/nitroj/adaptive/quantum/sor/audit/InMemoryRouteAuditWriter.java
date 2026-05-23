package com.nitroj.adaptive.quantum.sor.audit;

/**
 * Responsibility: append route audit events to an in-memory buffer.
 *
 * <p>Role in system: Phase 1 execution and tests use this compact writer before
 * durable audit persistence is introduced.</p>
 *
 * <p>Relationships: delegates storage behavior to {@link RouteAuditEventBuffer}.</p>
 *
 * <p>Lifecycle: long-lived writer created with engine context.</p>
 *
 * <p>Design intent: append returns false on strict full-buffer rejection rather
 * than formatting narrative strings or throwing in the hot path.</p>
 */
public final class InMemoryRouteAuditWriter implements RouteAuditWriter {
    private final RouteAuditEventBuffer buffer;

    public InMemoryRouteAuditWriter(final RouteAuditEventBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.buffer = buffer;
    }

    @Override
    public boolean append(final RouteAuditEvent event) {
        return buffer.append(event);
    }

    public RouteAuditEventBuffer buffer() {
        return buffer;
    }
}
