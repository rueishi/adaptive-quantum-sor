package com.nitroj.sor.core.intake;

import com.nitroj.sor.core.SorEngineImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies shutdown drain behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
class ShutdownDrainTest {
    @Test
    void closeAfterQueuedOrdersCompletesWithinDrainTimeout() {
        final SorEngineImpl engine = (SorEngineImpl) IntakeTestSupport.builder(8).build();
        engine.warmup(0);
        for (int i = 0; i < 4; i++) {
            engine.submitParentOrder(IntakeTestSupport.request(1));
        }

        assertDoesNotThrow(engine::close);
    }
}
