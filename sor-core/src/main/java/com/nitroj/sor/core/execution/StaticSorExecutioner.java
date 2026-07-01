package com.nitroj.sor.core.execution;

import com.nitroj.sor.core.audit.RouteAuditEvent;
import com.nitroj.sor.core.metadata.FeeScheduleSnapshot;
import com.nitroj.sor.core.model.ChildOrderBuffer;
import com.nitroj.sor.core.model.OrderIntent;
import com.nitroj.sor.core.model.OrderStatus;
import com.nitroj.sor.core.model.Side;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.state.VenueSessionState;

/**
 * Responsibility: provide a simple static SOR baseline.
 *
 * <p>Role in system: comparison metrics use this deterministic baseline to
 * evaluate adaptive policy SOR behavior on identical simulated inputs.</p>
 *
 * <p>Relationships: reads market data, fee schedule, venue session, and risk
 * limits; writes the same {@link ChildOrderBuffer} shape as adaptive routing.</p>
 *
 * <p>Lifecycle: created during comparison setup and called per parent order.</p>
 *
 * <p>Design intent: intentionally simple fee-adjusted top-of-book ranking keeps
 * baseline behavior stable as adaptive features evolve.</p>
 */
public final class StaticSorExecutioner implements SorExecutioner {
    private final MarketBookState marketBookState;
    private final FeeScheduleSnapshot feeSchedule;
    private final VenueSessionState venueSessionState;
    private final RiskLimitSnapshot riskLimits;
    private final int venueCount;
    private long nextChildOrderId = 1L;
    private long nextAuditEventId = 1L;

    public StaticSorExecutioner(
            final int venueCount,
            final MarketBookState marketBookState,
            final FeeScheduleSnapshot feeSchedule,
            final VenueSessionState venueSessionState,
            final RiskLimitSnapshot riskLimits
    ) {
        if (venueCount <= 0 || marketBookState == null || feeSchedule == null || venueSessionState == null || riskLimits == null) {
            throw new IllegalArgumentException("static SOR dependencies must be valid");
        }
        this.venueCount = venueCount;
        this.marketBookState = marketBookState;
        this.feeSchedule = feeSchedule;
        this.venueSessionState = venueSessionState;
        this.riskLimits = riskLimits;
    }

    /** Routes by best fee-adjusted price, then venue ID. */
    @Override
    public RouteDecisionResult route(final OrderIntent intent, final ChildOrderBuffer output) {
        if (intent == null || output == null) {
            throw new IllegalArgumentException("intent and output must not be null");
        }
        output.reset();
        long remaining = intent.quantity;
        final boolean[] used = new boolean[venueCount];
        while (remaining > 0) {
            final int venueId = bestVenue(intent, used);
            if (venueId < 0) {
                break;
            }
            used[venueId] = true;
            final long liquidity = intent.side == Side.BUY
                    ? marketBookState.askQty(intent.instrumentId, venueId)
                    : marketBookState.bidQty(intent.instrumentId, venueId);
            final long childQty = Math.min(remaining, Math.min(liquidity, riskLimits.maxChildQty(intent.instrumentId)));
            if (childQty > 0) {
                output.add(nextChildOrderId++, intent.parentOrderId, intent.instrumentId, venueId, intent.side, childQty, 0L, 0L);
                remaining -= childQty;
            }
        }
        final long routed = intent.quantity - remaining;
        final int status = output.size() == 0 ? OrderStatus.NO_LIQUIDITY : OrderStatus.ACKED;
        final RouteAuditEvent audit = new RouteAuditEvent();
        audit.set(nextAuditEventId++, Math.max(1L, System.nanoTime()), intent.parentOrderId, 0L, 0L, output.size(), remaining, status);
        return new RouteDecisionResult(intent.parentOrderId, 0L, 0L, output.size(), routed, remaining, status, audit);
    }

    private int bestVenue(final OrderIntent intent, final boolean[] used) {
        int best = -1;
        long bestScore = intent.side == Side.BUY ? Long.MAX_VALUE : Long.MIN_VALUE;
        for (int venueId = 0; venueId < venueCount; venueId++) {
            if (used[venueId] || !venueSessionState.isAvailable(venueId)) {
                continue;
            }
            final long qty = intent.side == Side.BUY
                    ? marketBookState.askQty(intent.instrumentId, venueId)
                    : marketBookState.bidQty(intent.instrumentId, venueId);
            if (qty <= 0) {
                continue;
            }
            final long price = intent.side == Side.BUY
                    ? marketBookState.askPriceTicks(intent.instrumentId, venueId)
                    : marketBookState.bidPriceTicks(intent.instrumentId, venueId);
            final long score = intent.side == Side.BUY
                    ? price + feeSchedule.takerFeeTicks(intent.instrumentId, venueId)
                    : price - feeSchedule.takerFeeTicks(intent.instrumentId, venueId);
            if ((intent.side == Side.BUY && score < bestScore) || (intent.side == Side.SELL && score > bestScore)
                    || (score == bestScore && (best < 0 || venueId < best))) {
                bestScore = score;
                best = venueId;
            }
        }
        return best;
    }
}
