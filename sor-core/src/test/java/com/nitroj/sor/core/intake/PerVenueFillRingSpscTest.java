package com.nitroj.sor.core.intake;

import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Verifies per venue fill ring spsc behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
class PerVenueFillRingSpscTest {
    @Test
    void inboundFillRingIsSpscPerVenue() {
        final InboundFillRings rings = new InboundFillRings();

        final var venueOne = rings.ringForVenue(1);
        final var venueTwo = rings.ringForVenue(2);

        assertInstanceOf(OneToOneRingBuffer.class, venueOne);
        assertInstanceOf(OneToOneRingBuffer.class, venueTwo);
        assertNotSame(venueOne, venueTwo);
    }
}
