package com.nitroj.sor.core.abi;

/**
 * Responsibility: immutable view of a decoded HotRouteBook ABI v1 snapshot.
 *
 * <p>Role in system: lets tests and future recovery code inspect the binary
 * snapshot without reconstructing mutable engine internals.</p>
 *
 * <p>Relationships: returned by {@link HotRouteBookAbiV1Reader}.</p>
 *
 * <p>Lifecycle: created after successful CRC and header validation.</p>
 *
 * <p>Design intent: preserve payload bytes and decoded arrays for byte-perfect
 * round-trip tests.</p>
 */
public record HotRouteBookSnapshot(
        int instrumentCount,
        int venueCount,
        int regimeCount,
        int urgencyCount,
        long policyVersion,
        long policyHash64,
        long effectiveFromEpochNanos,
        int[] routeStart,
        int[] routeEnd,
        int[] routeVenueId,
        long[] maxChildQty,
        int[] venueWeightBps,
        int[] participationCapBps
) {
}
