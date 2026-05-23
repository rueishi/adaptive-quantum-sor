package com.nitroj.sor.codec.v1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableLengthStringBoundaryTest {
    @Test
    void boundedStringRoundTripsAtMaxLength() {
        final String text = "x".repeat(SorProtocolV1Codec.MAX_TEXT_LENGTH);
        final SorProtocolMessage decoded = SorProtocolV1Codec.decode(SorProtocolV1Codec.encode(
                new SorProtocolMessage(SorMessageType.RejectedEvent, new long[] {1}, new int[] {2}, text)));

        assertEquals(text, decoded.text());
    }

    @Test
    void exceedingMaxLengthFailsClearly() {
        assertThrows(IllegalArgumentException.class, () -> new SorProtocolMessage(
                SorMessageType.RejectedEvent, new long[] {1}, new int[] {2},
                "x".repeat(SorProtocolV1Codec.MAX_TEXT_LENGTH + 1)));
    }
}
