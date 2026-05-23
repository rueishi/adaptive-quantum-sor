package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies v1 reserves 64 zeroed header bytes for future versions. */
class HotRouteBookAbiV1ReservedPaddingTest {
    @Test
    void reservedHeaderBytesArePresentAndZeroed() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        for (int i = 0; i < HotRouteBookAbiV1.RESERVED_HEADER_BYTES; i++) {
            assertEquals(0, bytes[HotRouteBookAbiV1.RESERVED_HEADER_OFFSET + i]);
        }
    }
}
