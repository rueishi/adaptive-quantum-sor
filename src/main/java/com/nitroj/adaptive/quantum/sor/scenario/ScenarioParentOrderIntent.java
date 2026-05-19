package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.Side;

/** Parent-order intent supplied as part of a live scenario run request. */
public record ScenarioParentOrderIntent(int instrumentId, int side, long quantity, int urgencyId) {
    public ScenarioParentOrderIntent {
        if (instrumentId < 0 || !Side.isValid(side) || quantity <= 0 || urgencyId < 0) {
            throw new IllegalArgumentException("invalid scenario parent order intent");
        }
    }

    public OrderIntent toIntent(final long parentOrderId, final long nowNanos) {
        return new OrderIntent(parentOrderId, instrumentId, side, quantity, urgencyId, nowNanos);
    }
}
