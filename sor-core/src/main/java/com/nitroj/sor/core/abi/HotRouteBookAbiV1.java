package com.nitroj.sor.core.abi;

/**
 * Responsibility: ABI v1 layout constants for serialized HotRouteBook
 * snapshots.
 *
 * <p>Role in system: centralizes immutable offsets and sizes used by the
 * writer, reader, docs, and no-silent-change tests.</p>
 *
 * <p>Relationships: consumed by {@link HotRouteBookAbiV1Writer} and
 * {@link HotRouteBookAbiV1Reader}.</p>
 *
 * <p>Lifecycle: constants are loaded with `sor-core`; values are frozen for ABI
 * version 1.</p>
 *
 * <p>Design intent: no computed layout methods live here so any offset change
 * is obvious in review.</p>
 */
public final class HotRouteBookAbiV1 {
    public static final long MAGIC = 0x5152534f48524231L;
    public static final int VERSION = 1;
    public static final int MAGIC_OFFSET = 0;
    public static final int VERSION_OFFSET = 8;
    public static final int HEADER_FLAGS_OFFSET = 12;
    public static final int INSTRUMENT_COUNT_OFFSET = 16;
    public static final int VENUE_COUNT_OFFSET = 20;
    public static final int REGIME_COUNT_OFFSET = 24;
    public static final int URGENCY_COUNT_OFFSET = 28;
    public static final int POLICY_VERSION_OFFSET = 32;
    public static final int POLICY_HASH64_OFFSET = 40;
    public static final int EFFECTIVE_FROM_EPOCH_NANOS_OFFSET = 48;
    public static final int ROUTE_ENTRY_COUNT_OFFSET = 56;
    public static final int RESERVED_HEADER_OFFSET = 64;
    public static final int RESERVED_HEADER_BYTES = 64;
    public static final int HEADER_BYTES = 128;
    public static final int INT_BYTES = 4;
    public static final int LONG_BYTES = 8;
    public static final int CRC_BYTES = 4;

    private HotRouteBookAbiV1() {
    }
}
