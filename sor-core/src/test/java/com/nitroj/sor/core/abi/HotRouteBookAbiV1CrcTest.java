package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies CRC-32C corruption detection for ABI v1 buffers. */
class HotRouteBookAbiV1CrcTest {
    @Test
    void corruptedPayloadFailsCrc() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        bytes[HotRouteBookAbiV1.HEADER_BYTES] ^= 0x01;

        final AbiException ex = assertThrows(AbiException.class, () -> HotRouteBookAbiV1Reader.read(ByteBuffer.wrap(bytes)));
        assertEquals("CRC_MISMATCH", ex.reason());
    }
}
