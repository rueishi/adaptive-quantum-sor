package com.nitroj.sor.core.policy;

import com.nitroj.sor.core.util.Indexing;

/**
 * Responsibility: hold the pre-ranked route lists consumed by the CPU SOR.
 *
 * <p>Role in system: this is the compact, read-only execution view of a
 * published policy. The executioner resolves one instrument/regime/urgency
 * route key, reads the matching offset range, then walks aligned venue and
 * parameter arrays without recomputing optimizer scores.</p>
 *
 * <p>Relationships: {@link SorPolicy} owns one hot route book, while
 * {@link FullPolicyMatrix} keeps the broader audit and replay matrix. The
 * canonical key and offset formulas mirror {@link Indexing}.</p>
 *
 * <p>Lifecycle: created by the policy compiler, validated at construction, and
 * never mutated after policy publication.</p>
 *
 * <p>Design intent: arrays remain exposed final references because later hot
 * path code needs primitive array access. Construction validation catches
 * malformed candidates before the arrays are treated as frozen.</p>
 */
public final class HotRouteBook {
    public final int instrumentCount;
    public final int regimeCount;
    public final int urgencyCount;

    public final int[] routeListOffset;

    public final short[] routeVenueId;
    public final short[] routeFlags;

    public final int[] weightBps;
    public final int[] latencyPenaltyNanos;
    public final int[] toxicityPenaltyBps;
    public final int[] fillProbabilityBps;
    public final int[] rejectPenaltyBps;
    public final int[] queueSurvivalBps;
    public final int[] feePenaltyTicks;
    public final int[] slippagePenaltyBps;
    public final int[] marketImpactPenaltyBps;

    public final long[] minChildQty;
    public final long[] maxChildQty;
    public final long[] maxVenueNotional;
    public final int[] maxParticipationBps;

    /**
     * Creates a validated immutable-publication route book.
     *
     * @param instrumentCount dense instrument count
     * @param regimeCount dense regime count
     * @param urgencyCount dense urgency count
     * @param routeListOffset offset array with one sentinel entry
     * @param routeVenueId flattened route venue IDs
     * @param routeFlags flattened route flags aligned with venues
     */
    public HotRouteBook(
            final int instrumentCount,
            final int regimeCount,
            final int urgencyCount,
            final int[] routeListOffset,
            final short[] routeVenueId,
            final short[] routeFlags,
            final int[] weightBps,
            final int[] latencyPenaltyNanos,
            final int[] toxicityPenaltyBps,
            final int[] fillProbabilityBps,
            final int[] rejectPenaltyBps,
            final int[] queueSurvivalBps,
            final int[] feePenaltyTicks,
            final int[] slippagePenaltyBps,
            final int[] marketImpactPenaltyBps,
            final long[] minChildQty,
            final long[] maxChildQty,
            final long[] maxVenueNotional,
            final int[] maxParticipationBps
    ) {
        this.instrumentCount = requirePositive("instrumentCount", instrumentCount);
        this.regimeCount = requirePositive("regimeCount", regimeCount);
        this.urgencyCount = requirePositive("urgencyCount", urgencyCount);
        final int requiredOffsets = instrumentCount * regimeCount * urgencyCount + 1;
        if (routeListOffset == null || routeListOffset.length != requiredOffsets) {
            throw new IllegalArgumentException("routeListOffset length must equal instrumentCount * regimeCount * urgencyCount + 1");
        }
        this.routeVenueId = requireShortArray("routeVenueId", routeVenueId);
        final int routeLength = routeVenueId.length;
        this.routeListOffset = routeListOffset;
        this.routeFlags = requireShortArray("routeFlags", routeFlags, routeLength);
        this.weightBps = requireIntArray("weightBps", weightBps, routeLength);
        this.latencyPenaltyNanos = requireIntArray("latencyPenaltyNanos", latencyPenaltyNanos, routeLength);
        this.toxicityPenaltyBps = requireIntArray("toxicityPenaltyBps", toxicityPenaltyBps, routeLength);
        this.fillProbabilityBps = requireIntArray("fillProbabilityBps", fillProbabilityBps, routeLength);
        this.rejectPenaltyBps = requireIntArray("rejectPenaltyBps", rejectPenaltyBps, routeLength);
        this.queueSurvivalBps = requireIntArray("queueSurvivalBps", queueSurvivalBps, routeLength);
        this.feePenaltyTicks = requireIntArray("feePenaltyTicks", feePenaltyTicks, routeLength);
        this.slippagePenaltyBps = requireIntArray("slippagePenaltyBps", slippagePenaltyBps, routeLength);
        this.marketImpactPenaltyBps = requireIntArray("marketImpactPenaltyBps", marketImpactPenaltyBps, routeLength);
        this.minChildQty = requireLongArray("minChildQty", minChildQty, routeLength);
        this.maxChildQty = requireLongArray("maxChildQty", maxChildQty, routeLength);
        this.maxVenueNotional = requireLongArray("maxVenueNotional", maxVenueNotional, routeLength);
        this.maxParticipationBps = requireIntArray("maxParticipationBps", maxParticipationBps, routeLength);
        validateOffsets(routeListOffset, routeLength);
    }

    /**
     * Computes the canonical hot-route key for route-list lookup.
     *
     * @return {@code ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId}
     */
    public int routeKey(final int instrumentId, final int regimeId, final int urgencyId) {
        checkRange("instrumentId", instrumentId, instrumentCount);
        checkRange("regimeId", regimeId, regimeCount);
        checkRange("urgencyId", urgencyId, urgencyCount);
        return ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId;
    }

    /**
     * Returns the inclusive start offset for a route key.
     */
    public int routeStart(final int instrumentId, final int regimeId, final int urgencyId) {
        return routeListOffset[routeKey(instrumentId, regimeId, urgencyId)];
    }

    /**
     * Returns the exclusive end offset for a route key.
     */
    public int routeEnd(final int instrumentId, final int regimeId, final int urgencyId) {
        return routeListOffset[routeKey(instrumentId, regimeId, urgencyId) + 1];
    }

    private static void validateOffsets(final int[] offsets, final int routeLength) {
        if (offsets[0] != 0) {
            throw new IllegalArgumentException("routeListOffset must start at zero");
        }
        for (int i = 1; i < offsets.length; i++) {
            if (offsets[i] < offsets[i - 1]) {
                throw new IllegalArgumentException("routeListOffset must be monotonic");
            }
        }
        if (offsets[offsets.length - 1] != routeLength) {
            throw new IllegalArgumentException("routeListOffset sentinel must equal route array length");
        }
    }

    private static int requirePositive(final String name, final int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static short[] requireShortArray(final String name, final short[] value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    private static short[] requireShortArray(final String name, final short[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must align with routeVenueId");
        }
        return value;
    }

    private static int[] requireIntArray(final String name, final int[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must align with routeVenueId");
        }
        return value;
    }

    private static long[] requireLongArray(final String name, final long[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must align with routeVenueId");
        }
        return value;
    }

    private static void checkRange(final String name, final int value, final int upperExclusive) {
        if (value < 0 || value >= upperExclusive) {
            throw new IndexOutOfBoundsException(name + " out of range: " + value);
        }
    }
}
