package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.model.ChildOrder;

/**
 * Responsibility: expose parent and child order status through the API.
 *
 * <p>Role in system: GET /orders/{id} returns this control-plane view.</p>
 *
 * <p>Relationships: populated by {@link SorHttpApiServer} from accepted order
 * requests and execution results.</p>
 *
 * <p>Lifecycle: stored in memory for Phase 1 API tests.</p>
 *
 * <p>Design intent: simple JSON generation keeps the API dependency-free.</p>
 */
public final class OrderStatusView {
    public long parentOrderId;
    public long filledQty;
    public long remainingQty;
    public int status;
    public ChildOrder[] childOrders;

    /** Serializes the view to compact JSON. */
    public String toJson() {
        return "{\"parentOrderId\":" + parentOrderId
                + ",\"filledQty\":" + filledQty
                + ",\"remainingQty\":" + remainingQty
                + ",\"status\":" + status
                + ",\"childOrderCount\":" + (childOrders == null ? 0 : childOrders.length)
                + "}";
    }
}
