package com.nitroj.sor.core.intake;

import com.nitroj.sor.core.SorEngineImpl;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies concurrent submit no loss behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
class ConcurrentSubmitNoLossTest {
    @Test
    void concurrentProducersReceiveUniqueSequentialParentIds() throws Exception {
        final int producers = 8;
        final int perProducer = 1000;
        final SorEngineImpl engine = (SorEngineImpl) IntakeTestSupport.builder(16384).build();
        engine.warmup(0);
        final Set<Long> ids = new ConcurrentSkipListSet<>();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(producers);

        for (int p = 0; p < producers; p++) {
            new Thread(() -> {
                await(start);
                for (int i = 0; i < perProducer; i++) {
                    ids.add(engine.submitParentOrder(IntakeTestSupport.request(1)));
                }
                done.countDown();
            }, "submitter-" + p).start();
        }

        start.countDown();
        done.await();

        assertEquals(producers * perProducer, ids.size());
        assertEquals(1L, ids.iterator().next());
        assertEquals(producers * perProducer, ids.stream().mapToLong(Long::longValue).max().orElseThrow());
        engine.close();
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }
}
