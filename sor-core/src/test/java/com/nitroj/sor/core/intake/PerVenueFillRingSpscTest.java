package com.nitroj.sor.core.intake;

import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

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
