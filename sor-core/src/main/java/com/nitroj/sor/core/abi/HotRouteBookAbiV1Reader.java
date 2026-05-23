package com.nitroj.sor.core.abi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32C;

/**
 * Responsibility: validates and decodes HotRouteBook ABI v1 buffers.
 *
 * <p>Role in system: provides byte-level recovery and golden-test verification
 * without exposing raw buffer parsing throughout the engine.</p>
 *
 * <p>Relationships: mirrors {@link HotRouteBookAbiV1Writer} and returns
 * {@link HotRouteBookSnapshot}.</p>
 *
 * <p>Lifecycle: called on control-plane recovery/test paths, never while
 * routing.</p>
 *
 * <p>Design intent: reject corrupt or incompatible buffers with
 * {@link AbiException} instead of low-level buffer exceptions.</p>
 */
public final class HotRouteBookAbiV1Reader {
    private HotRouteBookAbiV1Reader() {
    }

    /**
     * Reads and validates one ABI v1 snapshot.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param source source buffer whose limit marks the snapshot end
     * @return decoded immutable snapshot
     */
    public static HotRouteBookSnapshot read(final ByteBuffer source) {
        final ByteBuffer buffer = source.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        requireRemaining(buffer, HotRouteBookAbiV1.HEADER_BYTES + HotRouteBookAbiV1.CRC_BYTES);

        final long magic = buffer.getLong(HotRouteBookAbiV1.MAGIC_OFFSET);
        if (magic != HotRouteBookAbiV1.MAGIC) {
            throw new AbiException("MAGIC_MISMATCH", "magic mismatch: " + Long.toHexString(magic));
        }
        final int version = buffer.getInt(HotRouteBookAbiV1.VERSION_OFFSET);
        if (version != HotRouteBookAbiV1.VERSION) {
            throw new AbiException("UNSUPPORTED_VERSION", "unsupported ABI version: " + version);
        }
        final int flags = buffer.getInt(HotRouteBookAbiV1.HEADER_FLAGS_OFFSET);
        if (flags != 0) {
            throw new AbiException("UNSUPPORTED_FLAGS", "unsupported header flags: " + flags);
        }

        final int instrumentCount = positive(buffer.getInt(HotRouteBookAbiV1.INSTRUMENT_COUNT_OFFSET), "instrumentCount");
        final int venueCount = positive(buffer.getInt(HotRouteBookAbiV1.VENUE_COUNT_OFFSET), "venueCount");
        final int regimeCount = positive(buffer.getInt(HotRouteBookAbiV1.REGIME_COUNT_OFFSET), "regimeCount");
        final int urgencyCount = positive(buffer.getInt(HotRouteBookAbiV1.URGENCY_COUNT_OFFSET), "urgencyCount");
        final long policyVersion = buffer.getLong(HotRouteBookAbiV1.POLICY_VERSION_OFFSET);
        final long policyHash64 = buffer.getLong(HotRouteBookAbiV1.POLICY_HASH64_OFFSET);
        final long effectiveFromEpochNanos = buffer.getLong(HotRouteBookAbiV1.EFFECTIVE_FROM_EPOCH_NANOS_OFFSET);
        final long routeEntryCountLong = buffer.getLong(HotRouteBookAbiV1.ROUTE_ENTRY_COUNT_OFFSET);
        if (routeEntryCountLong < 0 || routeEntryCountLong > Integer.MAX_VALUE) {
            throw new AbiException("INVALID_DIMENSIONS", "invalid routeEntryCount: " + routeEntryCountLong);
        }
        final int routeEntryCount = (int) routeEntryCountLong;
        final int routeKeyCount = instrumentCount * regimeCount * urgencyCount;
        final int requiredBytes = requiredBytes(routeKeyCount, routeEntryCount);
        requireRemaining(buffer, requiredBytes);
        verifyCrc(buffer, requiredBytes);

        int offset = HotRouteBookAbiV1.HEADER_BYTES;
        final int[] routeStart = new int[routeKeyCount];
        final int[] routeEnd = new int[routeKeyCount];
        final int[] routeVenueId = new int[routeEntryCount];
        final long[] maxChildQty = new long[routeEntryCount];
        final int[] venueWeightBps = new int[routeEntryCount];
        final int[] participationCapBps = new int[routeEntryCount];

        for (int i = 0; i < routeKeyCount; i++) {
            routeStart[i] = buffer.getInt(offset);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int i = 0; i < routeKeyCount; i++) {
            routeEnd[i] = buffer.getInt(offset);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int i = 0; i < routeEntryCount; i++) {
            routeVenueId[i] = buffer.getInt(offset);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int i = 0; i < routeEntryCount; i++) {
            maxChildQty[i] = buffer.getLong(offset);
            offset += HotRouteBookAbiV1.LONG_BYTES;
        }
        for (int i = 0; i < routeEntryCount; i++) {
            venueWeightBps[i] = buffer.getInt(offset);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int i = 0; i < routeEntryCount; i++) {
            participationCapBps[i] = buffer.getInt(offset);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }

        return new HotRouteBookSnapshot(instrumentCount, venueCount, regimeCount, urgencyCount,
                policyVersion, policyHash64, effectiveFromEpochNanos, routeStart, routeEnd,
                routeVenueId, maxChildQty, venueWeightBps, participationCapBps);
    }

    private static int positive(final int value, final String field) {
        if (value <= 0) {
            throw new AbiException("INVALID_DIMENSIONS", field + " must be positive");
        }
        return value;
    }

    private static void requireRemaining(final ByteBuffer buffer, final int requiredBytes) {
        if (buffer.limit() < requiredBytes) {
            throw new AbiException("TRUNCATED", "truncated buffer: need " + requiredBytes + " bytes, found " + buffer.limit());
        }
    }

    private static void verifyCrc(final ByteBuffer buffer, final int requiredBytes) {
        final int crcOffset = requiredBytes - HotRouteBookAbiV1.CRC_BYTES;
        final CRC32C crc = new CRC32C();
        crc.update(buffer.slice(0, crcOffset));
        final int expected = buffer.getInt(crcOffset);
        final int actual = (int) crc.getValue();
        if (expected != actual) {
            throw new AbiException("CRC_MISMATCH", "CRC mismatch");
        }
    }

    private static int requiredBytes(final int routeKeyCount, final int routeEntryCount) {
        return HotRouteBookAbiV1.HEADER_BYTES
                + routeKeyCount * HotRouteBookAbiV1.INT_BYTES
                + routeKeyCount * HotRouteBookAbiV1.INT_BYTES
                + routeEntryCount * HotRouteBookAbiV1.INT_BYTES
                + routeEntryCount * HotRouteBookAbiV1.LONG_BYTES
                + routeEntryCount * HotRouteBookAbiV1.INT_BYTES
                + routeEntryCount * HotRouteBookAbiV1.INT_BYTES
                + HotRouteBookAbiV1.CRC_BYTES;
    }
}
