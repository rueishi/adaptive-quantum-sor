package com.nitroj.adaptive.quantum.sor.audit;

/**
 * Responsibility: define compact route audit event writing.
 *
 * <p>Role in system: execution code records primitive audit events independent
 * of narrative logging.</p>
 *
 * <p>Relationships: implemented by {@link InMemoryRouteAuditWriter}.</p>
 *
 * <p>Lifecycle: created at startup and called per route decision.</p>
 *
 * <p>Design intent: keep audit append behavior explicit and testable.</p>
 */
public interface RouteAuditWriter {
    /** Appends one route audit event. */
    boolean append(RouteAuditEvent event);
}
