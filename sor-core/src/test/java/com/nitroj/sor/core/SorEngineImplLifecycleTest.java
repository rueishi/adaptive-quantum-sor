package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-06 SorEngine lifecycle semantics. */
class SorEngineImplLifecycleTest {
    @Test
    void warmupReadinessSubmitStatusAndCloseWork() {
        final SorEngine engine = EngineTestSupport.engine();
        final ParentOrderRequest request = ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(10).urgency(0).build();

        assertFalse(engine.isReady());
        assertThrows(IllegalStateException.class, () -> engine.submitParentOrder(request));
        engine.warmup(1000);
        assertTrue(engine.isReady());

        final long first = engine.submitParentOrder(request);
        final long second = engine.submitParentOrder(request);
        assertTrue(first > 0);
        assertEquals(first + 1, second);
        assertEquals(OrderStatusCode.ACCEPTED, engine.getOrderStatus(first).orElseThrow().status());

        engine.close();
        assertThrows(IllegalStateException.class, () -> engine.submitParentOrder(request));
    }

    @Test
    void implementationIsPublicApiEngine() {
        assertInstanceOf(SorEngine.class, EngineTestSupport.engine());
    }
}
