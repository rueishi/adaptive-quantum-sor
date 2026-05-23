package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies multibyte ABI v1 fields are little-endian. */
class HotRouteBookAbiV1EndiannessTest {
    @Test
    void instrumentCountIsLittleEndian() {
        final byte[] bytes = HotRouteBookAbiTestFixtures.serialized();
        assertEquals(2, bytes[HotRouteBookAbiV1.INSTRUMENT_COUNT_OFFSET]);
        assertEquals(0, bytes[HotRouteBookAbiV1.INSTRUMENT_COUNT_OFFSET + 3]);
    }
}
