package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies status reads match submitted order lifecycle state. */
class OrderStatusConsistencyTest {
    @Test
    void statusMatchesSubmission() {
        final var engine = EngineTestSupport.engine();
        engine.warmup(1);
        final long id = engine.submitParentOrder(ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(3).urgency(0).build());
        assertEquals(OrderStatusCode.ACCEPTED, engine.getOrderStatus(id).orElseThrow().status());
    }
}
