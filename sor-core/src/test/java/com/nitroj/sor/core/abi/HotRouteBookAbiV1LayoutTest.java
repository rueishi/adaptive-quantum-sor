package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies ABI v1 offset constants match the Phase 8 specification. */
class HotRouteBookAbiV1LayoutTest {
    @Test
    void layoutConstantsMatchSpec() {
        assertEquals(0x5152534f48524231L, HotRouteBookAbiV1.MAGIC);
        assertEquals(1, HotRouteBookAbiV1.VERSION);
        assertEquals(0, HotRouteBookAbiV1.MAGIC_OFFSET);
        assertEquals(8, HotRouteBookAbiV1.VERSION_OFFSET);
        assertEquals(12, HotRouteBookAbiV1.HEADER_FLAGS_OFFSET);
        assertEquals(16, HotRouteBookAbiV1.INSTRUMENT_COUNT_OFFSET);
        assertEquals(20, HotRouteBookAbiV1.VENUE_COUNT_OFFSET);
        assertEquals(24, HotRouteBookAbiV1.REGIME_COUNT_OFFSET);
        assertEquals(28, HotRouteBookAbiV1.URGENCY_COUNT_OFFSET);
        assertEquals(32, HotRouteBookAbiV1.POLICY_VERSION_OFFSET);
        assertEquals(40, HotRouteBookAbiV1.POLICY_HASH64_OFFSET);
        assertEquals(48, HotRouteBookAbiV1.EFFECTIVE_FROM_EPOCH_NANOS_OFFSET);
        assertEquals(56, HotRouteBookAbiV1.ROUTE_ENTRY_COUNT_OFFSET);
        assertEquals(64, HotRouteBookAbiV1.RESERVED_HEADER_OFFSET);
        assertEquals(64, HotRouteBookAbiV1.RESERVED_HEADER_BYTES);
        assertEquals(128, HotRouteBookAbiV1.HEADER_BYTES);
    }
}
