package com.nitroj.sor.api;

/**
 * Responsibility: public venue session state emitted through
 * {@link SorEvent.SessionStatusChanged}.
 *
 * <p>Role in system: communicates whether a venue can currently receive child
 * orders without exposing the engine's internal session-state classes.</p>
 *
 * <p>Relationships: used by event listeners and future venue adapters.</p>
 *
 * <p>Lifecycle: enum values are stable API constants.</p>
 *
 * <p>Design intent: keep session-change events strongly typed and exhaustive
 * for integrators.</p>
 */
public enum VenueStatus {
    OPEN,
    CLOSED,
    HALTED,
    DEGRADED
}
