package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.Side;

/** Parent-order intent supplied as part of a live scenario run request. */
public record ScenarioParentOrderIntent(
        int instrumentId,
        int side,
        long quantity,
        int urgencyId,
        int atTick,
        ScenarioParentOrderSubmitMode submitMode,
        String clientOrderRef
) {
    public ScenarioParentOrderIntent {
        if (instrumentId < 0 || !Side.isValid(side) || quantity <= 0 || urgencyId < 0 || atTick < 0) {
            throw new IllegalArgumentException("invalid scenario parent order intent");
        }
        if (submitMode == null) {
            throw new IllegalArgumentException("submitMode must not be null");
        }
        clientOrderRef = clientOrderRef == null ? "" : clientOrderRef;
    }

    public ScenarioParentOrderIntent(final int instrumentId, final int side, final long quantity, final int urgencyId) {
        this(instrumentId, side, quantity, urgencyId, 0, ScenarioParentOrderSubmitMode.SIMULATED, "");
    }

    public OrderIntent toIntent(final long parentOrderId, final long nowNanos) {
        return new OrderIntent(parentOrderId, instrumentId, side, quantity, urgencyId, nowNanos);
    }

    public String toJson() {
        return "{\"instrumentId\":" + instrumentId
                + ",\"side\":" + side
                + ",\"quantity\":" + quantity
                + ",\"urgencyId\":" + urgencyId
                + ",\"atTick\":" + atTick
                + ",\"submitMode\":\"" + submitMode + "\""
                + ",\"clientOrderRef\":\"" + escape(clientOrderRef) + "\"}";
    }

    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
