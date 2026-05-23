package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies truncated ABI v1 buffers fail with AbiException. */
class HotRouteBookAbiV1TruncationTest {
    @Test
    void truncatedBufferIsRejected() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        final ByteBuffer truncated = ByteBuffer.wrap(bytes, 0, bytes.length - 3);

        final AbiException ex = assertThrows(AbiException.class, () -> HotRouteBookAbiV1Reader.read(truncated));
        assertEquals("TRUNCATED", ex.reason());
    }
}
