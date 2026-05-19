package com.nitroj.adaptive.quantum.sor.benchmark;

import com.nitroj.adaptive.quantum.sor.execution.StaticSorExecutioner;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

/**
 * Responsibility: provide fixed benchmark inputs.
 *
 * <p>Role in system: benchmark classes use this fixture so latency/allocation
 * measurements run against deterministic market, fee, risk, and order inputs.</p>
 *
 * <p>Relationships: creates the static SOR benchmark dependencies. Adaptive
 * policy benchmarking reuses the same order and market shape in later tuning.</p>
 *
 * <p>Lifecycle: created once per benchmark run.</p>
 *
 * <p>Design intent: avoid benchmark setup in measured methods.</p>
 */
public final class BenchmarkFixture {
    public final MarketBookState marketBookState = new MarketBookState(1, 3);
    public final VenueSessionState venueSessionState = new VenueSessionState(3);
    public final RiskLimitSnapshot riskLimitSnapshot = new RiskLimitSnapshot(1, 3);
    public final FeeScheduleSnapshot feeScheduleSnapshot = new FeeScheduleSnapshot(1, 3);
    public final OrderIntent parentOrder = new OrderIntent(1L, 0, Side.BUY, 100L, 0, 1L);
    public final ChildOrderBuffer childOrderBuffer = new ChildOrderBuffer(4);

    public BenchmarkFixture() {
        for (int venueId = 0; venueId < 3; venueId++) {
            marketBookState.updateTopOfBook(0, venueId, 100, 101 + venueId, 1_000, 1_000);
            venueSessionState.setStatus(venueId, com.nitroj.adaptive.quantum.sor.model.VenueStatus.OPEN);
            riskLimitSnapshot.setVenueLimits(0, venueId, 1_000_000L, 2_500);
            feeScheduleSnapshot.setFees(0, venueId, 0, venueId);
        }
        riskLimitSnapshot.setMaxChildQty(0, 1_000L);
    }

    public StaticSorExecutioner staticSorExecutioner() {
        return new StaticSorExecutioner(3, marketBookState, feeScheduleSnapshot, venueSessionState, riskLimitSnapshot);
    }
}
