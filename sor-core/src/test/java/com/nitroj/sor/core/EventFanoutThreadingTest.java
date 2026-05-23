package com.nitroj.sor.core;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorEngine;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies events are delivered on the dedicated fanout thread. */
class EventFanoutThreadingTest {
    @Test
    void listenerRunsOnFanoutThread() throws Exception {
        final SorEngine engine = EngineTestSupport.engine();
        final ArrayBlockingQueue<String> threads = new ArrayBlockingQueue<>(4);
        engine.registerListener(event -> threads.offer(Thread.currentThread().getName()));
        engine.warmup(1);
        engine.submitParentOrder(ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(1).urgency(0).build());
        final String thread = threads.poll(2, TimeUnit.SECONDS);
        assertNotNull(thread);
        assertTrue(thread.startsWith("sor-event-fanout"));
    }
}
