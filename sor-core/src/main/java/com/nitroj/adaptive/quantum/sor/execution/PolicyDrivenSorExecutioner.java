package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.audit.RouteAuditEvent;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

/**
 * Responsibility: route parent orders using the active immutable policy.
 *
 * <p>Role in system: this is the Phase 1 adaptive CPU SOR execution path. It
 * captures one active policy at decision start and walks pre-ranked hot-route
 * arrays.</p>
 *
 * <p>Relationships: reads {@link PolicyPublisher}, {@link MarketBookState},
 * {@link VenueSessionState}, and {@link RiskLimitSnapshot}; writes
 * {@link ChildOrderBuffer} and {@link RouteAuditEvent}.</p>
 *
 * <p>Lifecycle: created once with state references and called per parent order
 * or reslice decision.</p>
 *
 * <p>Design intent: execution performs only simple live checks and never calls
 * optimizer code.</p>
 */
public final class PolicyDrivenSorExecutioner implements SorExecutioner {
    private final PolicyPublisher publisher;
    private final MarketBookState marketBookState;
    private final VenueSessionState venueSessionState;
    private final RiskLimitSnapshot riskLimits;
    private long nextChildOrderId = 1L;
    private long nextAuditEventId = 1L;

    public PolicyDrivenSorExecutioner(
            final PolicyPublisher publisher,
            final MarketBookState marketBookState,
            final VenueSessionState venueSessionState,
            final RiskLimitSnapshot riskLimits
    ) {
        if (publisher == null || marketBookState == null || venueSessionState == null || riskLimits == null) {
            throw new IllegalArgumentException("execution dependencies must not be null");
        }
        this.publisher = publisher;
        this.marketBookState = marketBookState;
        this.venueSessionState = venueSessionState;
        this.riskLimits = riskLimits;
    }

    /** Routes using regime 0 and the order's urgency for Phase 1. */
    @Override
    public RouteDecisionResult route(final OrderIntent intent, final ChildOrderBuffer output) {
        final RouteAuditEvent audit = new RouteAuditEvent();
        final MutableRouteDecisionResult result = new MutableRouteDecisionResult();
        routeInto(intent, output, result, audit, 0);
        return result.snapshot();
    }

    /** Routes using an explicit regime ID for reslicing tests. */
    public RouteDecisionResult route(final OrderIntent intent, final ChildOrderBuffer output, final int regimeId) {
        final RouteAuditEvent audit = new RouteAuditEvent();
        final MutableRouteDecisionResult result = new MutableRouteDecisionResult();
        routeInto(intent, output, result, audit, regimeId);
        return result.snapshot();
    }

    /**
     * Routes into caller-owned result and audit objects.
     *
     * <p>This is the strict allocation-sensitive API. The existing
     * {@link #route(OrderIntent, ChildOrderBuffer)} method remains convenient for
     * API/tests and intentionally wraps this method in immutable objects.</p>
     */
    public MutableRouteDecisionResult routeInto(
            final OrderIntent intent,
            final ChildOrderBuffer output,
            final MutableRouteDecisionResult result,
            final RouteAuditEvent audit
    ) {
        return routeInto(intent, output, result, audit, 0);
    }

    /**
     * Routes into caller-owned result and audit objects using an explicit regime
     * ID.
     */
    public MutableRouteDecisionResult routeInto(
            final OrderIntent intent,
            final ChildOrderBuffer output,
            final MutableRouteDecisionResult result,
            final RouteAuditEvent audit,
            final int regimeId
    ) {
        if (intent == null || output == null || result == null || audit == null) {
            throw new IllegalArgumentException("intent, output, result, and audit must not be null");
        }
        output.reset();
        final SorPolicy policy = publisher.activePolicy();
        if (policy == null) {
            return result(result, audit, intent, 0L, 0L, output.size(), 0L, intent.quantity, OrderStatus.NO_ACTIVE_POLICY);
        }
        long remaining = intent.quantity;
        final int start = policy.hotRouteBook.routeStart(intent.instrumentId, regimeId, intent.urgencyId);
        final int end = policy.hotRouteBook.routeEnd(intent.instrumentId, regimeId, intent.urgencyId);
        for (int i = start; i < end && remaining > 0; i++) {
            final int venueId = policy.hotRouteBook.routeVenueId[i];
            if (!venueSessionState.isAvailable(venueId)) {
                continue;
            }
            final long liquidity = intent.side == com.nitroj.adaptive.quantum.sor.model.Side.BUY
                    ? marketBookState.askQty(intent.instrumentId, venueId)
                    : marketBookState.bidQty(intent.instrumentId, venueId);
            final long childMax = Math.min(policy.hotRouteBook.maxChildQty[i], riskLimits.maxChildQty(intent.instrumentId));
            final long childQty = Math.min(remaining, Math.min(liquidity, childMax));
            if (childQty <= 0) {
                continue;
            }
            output.add(nextChildOrderId++, intent.parentOrderId, intent.instrumentId, venueId, intent.side,
                    childQty, policy.policyVersion, policy.policyHash64);
            remaining -= childQty;
        }
        final long routed = intent.quantity - remaining;
        final int status = output.size() == 0 ? OrderStatus.NO_LIQUIDITY : OrderStatus.ACKED;
        return result(result, audit, intent, policy.policyVersion, policy.policyHash64, output.size(), routed, remaining, status);
    }

    private MutableRouteDecisionResult result(
            final MutableRouteDecisionResult result,
            final RouteAuditEvent audit,
            final OrderIntent intent,
            final long policyVersion,
            final long policyHash64,
            final int childCount,
            final long routed,
            final long residual,
            final int status
    ) {
        audit.set(nextAuditEventId++, Math.max(1L, System.nanoTime()), intent.parentOrderId, policyVersion,
                policyHash64, childCount, residual, status);
        result.set(intent.parentOrderId, policyVersion, policyHash64, childCount, routed, residual, status, audit);
        return result;
    }
}
