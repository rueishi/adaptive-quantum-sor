package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies repeated cancel calls remain idempotent. */
class CancelIdempotencyTest {
    @Test
    void cancelTwiceLeavesSingleCancelledState() {
        final SorEngine engine = EngineTestSupport.engine();
        engine.warmup(1);
        final long id = engine.submitParentOrder(ParentOrderRequest.builder().instrumentId(0).side(Side.SELL).quantity(5).urgency(0).build());
        engine.cancelParentOrder(id);
        engine.cancelParentOrder(id);
        assertEquals(OrderStatusCode.CANCELLED, engine.getOrderStatus(id).orElseThrow().status());
    }
}
