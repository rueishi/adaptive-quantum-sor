package com.nitroj.sor.codec.v1;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SorProtocolV1RoundTripTest {
    @ParameterizedTest
    @MethodSource("com.nitroj.sor.codec.v1.ProtocolFixtures#messages")
    void everyMessageRoundTripsBytePerfect(final SorProtocolMessage message) {
        final byte[] encoded = SorProtocolV1Codec.encode(message);
        final SorProtocolMessage decoded = SorProtocolV1Codec.decode(encoded);

        assertEquals(message.type(), decoded.type());
        assertArrayEquals(message.longs(), decoded.longs());
        assertArrayEquals(message.ints(), decoded.ints());
        assertEquals(message.text(), decoded.text());
    }
}
