package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies layout constants cannot change silently without updating tests. */
class HotRouteBookAbiV1NoSilentChangesTest {
    @Test
    void publicStaticFinalConstantsMatchManifest() throws Exception {
        final Map<String, Long> expected = new LinkedHashMap<>();
        expected.put("MAGIC", 0x5152534f48524231L);
        expected.put("VERSION", 1L);
        expected.put("MAGIC_OFFSET", 0L);
        expected.put("VERSION_OFFSET", 8L);
        expected.put("HEADER_FLAGS_OFFSET", 12L);
        expected.put("INSTRUMENT_COUNT_OFFSET", 16L);
        expected.put("VENUE_COUNT_OFFSET", 20L);
        expected.put("REGIME_COUNT_OFFSET", 24L);
        expected.put("URGENCY_COUNT_OFFSET", 28L);
        expected.put("POLICY_VERSION_OFFSET", 32L);
        expected.put("POLICY_HASH64_OFFSET", 40L);
        expected.put("EFFECTIVE_FROM_EPOCH_NANOS_OFFSET", 48L);
        expected.put("ROUTE_ENTRY_COUNT_OFFSET", 56L);
        expected.put("RESERVED_HEADER_OFFSET", 64L);
        expected.put("RESERVED_HEADER_BYTES", 64L);
        expected.put("HEADER_BYTES", 128L);
        expected.put("INT_BYTES", 4L);
        expected.put("LONG_BYTES", 8L);
        expected.put("CRC_BYTES", 4L);

        for (Field field : HotRouteBookAbiV1.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                assertEquals(expected.get(field.getName()), ((Number) field.get(null)).longValue(), field.getName());
            }
        }
    }
}
