package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies ABI v1 rejects the wrong magic before payload reads. */
class HotRouteBookAbiV1MagicMismatchTest {
    @Test
    void wrongMagicIsRejected() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(HotRouteBookAbiV1.MAGIC_OFFSET, 1L);

        final AbiException ex = assertThrows(AbiException.class, () -> HotRouteBookAbiV1Reader.read(ByteBuffer.wrap(bytes)));
        assertEquals("MAGIC_MISMATCH", ex.reason());
    }
}
