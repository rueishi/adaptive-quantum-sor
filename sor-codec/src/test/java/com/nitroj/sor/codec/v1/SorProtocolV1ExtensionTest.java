package com.nitroj.sor.codec.v1;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies protocol v1 can tolerate documented extension fields.
 *
 * <p>Run with codec tests before changing message layouts or forward-compatibility handling.</p>
 */
class SorProtocolV1ExtensionTest {
    @Test
    void v1DecoderIgnoresTrailingMinorRevisionBytes() {
        final SorProtocolMessage message = new SorProtocolMessage(SorMessageType.SubmitParentOrder, new long[] {1}, new int[] {2}, "ok");
        final byte[] encoded = SorProtocolV1Codec.encode(message);
        final byte[] extended = Arrays.copyOf(encoded, encoded.length + 16);

        final SorProtocolMessage decoded = SorProtocolV1Codec.decode(extended);
        assertEquals(message.type(), decoded.type());
        assertArrayEquals(message.longs(), decoded.longs());
        assertArrayEquals(message.ints(), decoded.ints());
        assertEquals(message.text(), decoded.text());
    }
}
