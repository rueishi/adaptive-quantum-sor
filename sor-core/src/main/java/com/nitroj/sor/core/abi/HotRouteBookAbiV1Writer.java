package com.nitroj.sor.core.abi;

import com.nitroj.sor.core.policy.HotRouteBook;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32C;

/**
 * Responsibility: serializes a `HotRouteBook` into the frozen ABI v1 binary
 * layout.
 *
 * <p>Role in system: produces policy snapshot bytes for golden tests and later
 * persistence adapters.</p>
 *
 * <p>Relationships: uses {@link HotRouteBookAbiV1} constants and mirrors
 * {@link HotRouteBookAbiV1Reader}.</p>
 *
 * <p>Lifecycle: called on policy publication or test fixture generation, never
 * on the routing hot path.</p>
 *
 * <p>Design intent: deterministic little-endian output with a CRC-32C footer.</p>
 */
public final class HotRouteBookAbiV1Writer {
    private HotRouteBookAbiV1Writer() {
    }

    /**
     * Serializes without policy metadata.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    public static int write(final HotRouteBook book, final ByteBuffer target) {
        return write(book, 0, 0, 0, target);
    }

    /**
     * Serializes a route book and policy metadata into the target buffer.
     *
     * <p>Control-plane method, not hot-path. The caller owns buffer allocation
     * and capacity.</p>
     */
    public static int write(final HotRouteBook book, final long policyVersion, final long policyHash64,
                            final long effectiveFromEpochNanos, final ByteBuffer target) {
        final ByteBuffer buffer = target.order(ByteOrder.LITTLE_ENDIAN);
        final int routeKeyCount = book.instrumentCount * book.regimeCount * book.urgencyCount;
        final int routeEntryCount = book.routeVenueId.length;
        final int requiredBytes = requiredBytes(routeKeyCount, routeEntryCount);
        if (buffer.capacity() < requiredBytes) {
            throw new IllegalArgumentException("target capacity too small for HotRouteBook ABI v1");
        }
        buffer.clear();
        for (int i = 0; i < requiredBytes; i++) {
            buffer.put(i, (byte) 0);
        }

        buffer.putLong(HotRouteBookAbiV1.MAGIC_OFFSET, HotRouteBookAbiV1.MAGIC);
        buffer.putInt(HotRouteBookAbiV1.VERSION_OFFSET, HotRouteBookAbiV1.VERSION);
        buffer.putInt(HotRouteBookAbiV1.HEADER_FLAGS_OFFSET, 0);
        buffer.putInt(HotRouteBookAbiV1.INSTRUMENT_COUNT_OFFSET, book.instrumentCount);
        buffer.putInt(HotRouteBookAbiV1.VENUE_COUNT_OFFSET, venueCount(book));
        buffer.putInt(HotRouteBookAbiV1.REGIME_COUNT_OFFSET, book.regimeCount);
        buffer.putInt(HotRouteBookAbiV1.URGENCY_COUNT_OFFSET, book.urgencyCount);
        buffer.putLong(HotRouteBookAbiV1.POLICY_VERSION_OFFSET, policyVersion);
        buffer.putLong(HotRouteBookAbiV1.POLICY_HASH64_OFFSET, policyHash64);
        buffer.putLong(HotRouteBookAbiV1.EFFECTIVE_FROM_EPOCH_NANOS_OFFSET, effectiveFromEpochNanos);
        buffer.putLong(HotRouteBookAbiV1.ROUTE_ENTRY_COUNT_OFFSET, routeEntryCount);

        int offset = HotRouteBookAbiV1.HEADER_BYTES;
        for (int i = 0; i < routeKeyCount; i++) {
            buffer.putInt(offset, book.routeListOffset[i]);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int i = 0; i < routeKeyCount; i++) {
            buffer.putInt(offset, book.routeListOffset[i + 1]);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (short venueId : book.routeVenueId) {
            buffer.putInt(offset, venueId & 0xFFFF);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (long maxChildQty : book.maxChildQty) {
            buffer.putLong(offset, maxChildQty);
            offset += HotRouteBookAbiV1.LONG_BYTES;
        }
        for (int weight : book.weightBps) {
            buffer.putInt(offset, weight);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }
        for (int cap : book.maxParticipationBps) {
            buffer.putInt(offset, cap);
            offset += HotRouteBookAbiV1.INT_BYTES;
        }

        final CRC32C crc = new CRC32C();
        crc.update(buffer.slice(0, offset));
        buffer.putInt(offset, (int) crc.getValue());
        buffer.position(0);
        buffer.limit(requiredBytes);
        return requiredBytes;
    }

    /**
     * Computes the required byte count for a specific book shape.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    public static int requiredBytes(final HotRouteBook book) {
        return requiredBytes(book.instrumentCount * book.regimeCount * book.urgencyCount, book.routeVenueId.length);
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

    private static int venueCount(final HotRouteBook book) {
        int max = 0;
        for (short venue : book.routeVenueId) {
            max = Math.max(max, venue & 0xFFFF);
        }
        return max + 1;
    }
}
