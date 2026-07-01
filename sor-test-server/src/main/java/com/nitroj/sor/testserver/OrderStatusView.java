package com.nitroj.sor.testserver;

import com.nitroj.sor.core.model.ChildOrder;

/**
 * Responsibility: expose parent and child order status through the API.
 *
 * <p>Role in system: {@code GET /orders/{id}} returns this control-plane view
 * to notebooks and scenario tests. It gives users enough information to see
 * whether a demo parent order filled, left residual quantity, and produced
 * child orders without exposing the full internal engine state model.</p>
 *
 * <p>Relationships: populated by {@link NotebookScenarioHttpServer} from accepted order
 * requests and execution results. Child order details are counted from core
 * {@link ChildOrder} values, while the JSON surface remains intentionally
 * compact.</p>
 *
 * <p>Lifecycle: stored in the in-memory order map owned by a
 * {@link NotebookScenarioHttpServer} instance and serialized on demand for API
 * responses.</p>
 *
 * <p>Design intent: simple JSON generation keeps the test server
 * dependency-free and makes the notebook contract easy to inspect in tests.</p>
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
