package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies ABI v1 rejects unsupported version values. */
class HotRouteBookAbiV1VersionMismatchTest {
    @Test
    void unsupportedVersionIsRejected() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(HotRouteBookAbiV1.VERSION_OFFSET, 2);

        final AbiException ex = assertThrows(AbiException.class, () -> HotRouteBookAbiV1Reader.read(ByteBuffer.wrap(bytes)));
        assertEquals("UNSUPPORTED_VERSION", ex.reason());
    }
}
