package com.nitroj.sor.core.intake;

import com.nitroj.sor.core.SorEngineImpl;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies agrona ring buffer identity behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
class AgronaRingBufferIdentityTest {
    @Test
    void engineOrderIntakeFieldIsAgronaManyToOneRingBuffer() throws Exception {
        final SorEngineImpl engine = (SorEngineImpl) IntakeTestSupport.builder(8).build();
        final var field = SorEngineImpl.class.getDeclaredField("orderRingBuffer");
        field.setAccessible(true);

        assertInstanceOf(ManyToOneRingBuffer.class, field.get(engine));
        engine.close();
    }
}
