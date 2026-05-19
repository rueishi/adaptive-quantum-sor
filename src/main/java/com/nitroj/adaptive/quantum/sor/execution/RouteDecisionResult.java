package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.audit.RouteAuditEvent;

/**
 * Responsibility: summarize one SOR route decision.
 *
 * <p>Role in system: executioners return this object so tests, metrics, and API
 * code can inspect filled/routed/residual quantities without reading buffers
 * directly.</p>
 *
 * <p>Relationships: carries the compact {@link RouteAuditEvent} populated by
 * policy-driven or static executioners.</p>
 *
 * <p>Lifecycle: created per route or reslice decision.</p>
 *
 * <p>Design intent: immutable fields keep decision results stable after child
 * buffers are reused.</p>
 */
public final class RouteDecisionResult {
    public final long parentOrderId;
    public final long policyVersion;
    public final long policyHash64;
    public final int childOrderCount;
    public final long routedQty;
    public final long residualQty;
    public final int status;
    public final RouteAuditEvent auditEvent;

    public RouteDecisionResult(
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
}
