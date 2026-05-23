package com.nitroj.sor.core.abi;

import com.nitroj.adaptive.quantum.sor.policy.HotRouteBook;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies ABI v1 bytes decode and re-encode deterministically. */
class HotRouteBookAbiV1RoundTripTest {
    @Test
    void roundTripPreservesBytes() {
        final byte[] first = HotRouteBookAbiTestFixtures.serialized();
        final HotRouteBookSnapshot snapshot = HotRouteBookAbiV1Reader.read(ByteBuffer.wrap(first));

        assertEquals(2, snapshot.instrumentCount());
        assertEquals(3, snapshot.venueCount());
        assertEquals(42, snapshot.policyVersion());
        assertArrayEquals(new int[] {0, 3}, snapshot.routeStart());
        assertArrayEquals(new int[] {3, 6}, snapshot.routeEnd());

        final HotRouteBook book = HotRouteBookAbiTestFixtures.routeBook();
        final ByteBuffer secondBuffer = ByteBuffer.allocate(HotRouteBookAbiV1Writer.requiredBytes(book));
        final int bytes = HotRouteBookAbiV1Writer.write(book, 42, 1234, 5678, secondBuffer);
        final byte[] second = new byte[bytes];
        secondBuffer.get(second);
        assertArrayEquals(first, second);
    }
}
