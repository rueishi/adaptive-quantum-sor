package com.nitroj.sor.core.intake;

import com.nitroj.sor.api.BackpressureException;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.core.SorEngineImpl;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderQueueBackpressureTest {
    @Test
    void saturatedRingRejectsThenAcceptsAfterDrain() throws Exception {
        final SorEngineImpl engine = (SorEngineImpl) IntakeTestSupport.builder(2).build();
        final ArrayBlockingQueue<SorEvent> events = new ArrayBlockingQueue<>(8);
        engine.registerListener(events::offer);
        engine.warmup(0);

        engine.submitParentOrder(IntakeTestSupport.request(1));
        engine.submitParentOrder(IntakeTestSupport.request(1));

        assertThrows(BackpressureException.class, () -> engine.submitParentOrder(IntakeTestSupport.request(1)));
        assertInstanceOf(SorEvent.BackpressureRejected.class, pollBackpressure(events));

        assertEquals(2, engine.drainOrderIntake(10));
        engine.submitParentOrder(IntakeTestSupport.request(1));
        assertEquals(1, engine.orderRingDepth());
        engine.close();
    }

    private static SorEvent pollBackpressure(final ArrayBlockingQueue<SorEvent> events) throws InterruptedException {
        SorEvent event;
        do {
            event = events.poll(2, TimeUnit.SECONDS);
        } while (event != null && !(event instanceof SorEvent.BackpressureRejected));
        return event;
    }
}
