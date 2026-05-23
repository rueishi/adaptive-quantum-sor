package com.nitroj.adaptive.quantum.sor.audit;

/**
 * Responsibility: hold compact primitive audit data for one route decision.
 *
 * <p>Role in system: every execution decision must be traceable to a parent
 * order, policy identity, and output count. This compact event is separate from
 * human-readable lifecycle logging so routing does not build strings.</p>
 *
 * <p>Relationships: future audit writers append these events, and route
 * execution populates them alongside {@code ChildOrderBuffer} output.</p>
 *
 * <p>Lifecycle: events can be preallocated and reused by a buffer in later task
 * cards. This class currently exposes mutable primitive fields for that use.</p>
 *
 * <p>Design intent: compact audit state satisfies traceability requirements
 * without introducing logging overhead in the execution path.</p>
 */
public final class RouteAuditEvent {
    public long eventId;
    public long timestampNanos;
    public long parentOrderId;
    public long policyVersion;
    public long policyHash64;
    public int childOrderCount;
    public long residualQty;
    public int status;

    /**
     * Populates the compact audit event.
     *
     * @param eventId audit event identifier
     * @param timestampNanos event timestamp
     * @param parentOrderId parent order ID
     * @param policyVersion policy version captured for the decision
     * @param policyHash64 policy hash captured for the decision
     * @param childOrderCount number of child orders emitted
     * @param residualQty residual parent quantity
     * @param status route decision status
     */
    public void set(
            final long eventId,
            final long timestampNanos,
            final long parentOrderId,
            final long policyVersion,
            final long policyHash64,
            final int childOrderCount,
            final long residualQty,
            final int status
    ) {
        if (eventId <= 0 || parentOrderId <= 0) {
            throw new IllegalArgumentException("eventId and parentOrderId must be positive");
        }
        if (childOrderCount < 0 || residualQty < 0) {
            throw new IllegalArgumentException("childOrderCount and residualQty must be non-negative");
        }
        this.eventId = eventId;
        this.timestampNanos = timestampNanos;
        this.parentOrderId = parentOrderId;
        this.policyVersion = policyVersion;
        this.policyHash64 = policyHash64;
        this.childOrderCount = childOrderCount;
        this.residualQty = residualQty;
        this.status = status;
    }
}
