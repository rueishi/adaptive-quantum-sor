package com.nitroj.sor.core.execution;

import com.nitroj.sor.core.audit.RouteAuditEvent;

/**
 * Responsibility: hold one reusable route decision result.
 *
 * <p>Role in system: strict hot-path callers pass this object into
 * {@link PolicyDrivenSorExecutioner#routeInto} so routing can publish decision
 * fields without allocating an immutable wrapper per call.</p>
 *
 * <p>Relationships: mirrors {@link RouteDecisionResult} while keeping the
 * result object caller-owned and reusable.</p>
 *
 * <p>Lifecycle: allocated by benchmark or execution infrastructure before the
 * route loop and overwritten on every strict route decision.</p>
 *
 * <p>Design intent: primitive mutable fields make the allocation contract
 * explicit while preserving the existing immutable return type for non-strict
 * API flows.</p>
 */
public final class MutableRouteDecisionResult {
    public long parentOrderId;
    public long policyVersion;
    public long policyHash64;
    public int childOrderCount;
    public long routedQty;
    public long residualQty;
    public int status;
    public RouteAuditEvent auditEvent;

    public void set(
            final long parentOrderId,
            final long policyVersion,
            final long policyHash64,
            final int childOrderCount,
            final long routedQty,
            final long residualQty,
            final int status,
            final RouteAuditEvent auditEvent
    ) {
        this.parentOrderId = parentOrderId;
        this.policyVersion = policyVersion;
        this.policyHash64 = policyHash64;
        this.childOrderCount = childOrderCount;
        this.routedQty = routedQty;
        this.residualQty = residualQty;
        this.status = status;
        this.auditEvent = auditEvent;
    }

    public RouteDecisionResult snapshot() {
        return new RouteDecisionResult(parentOrderId, policyVersion, policyHash64, childOrderCount,
                routedQty, residualQty, status, auditEvent);
    }
}
