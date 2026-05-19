package com.nitroj.adaptive.quantum.sor.policy;

/**
 * Responsibility: store the full instrument/venue/regime/urgency policy matrix.
 *
 * <p>Role in system: unlike {@link HotRouteBook}, which is optimized for the
 * execution path, this structure preserves every candidate venue cell for
 * validation, diff generation, diagnostics, and replay.</p>
 *
 * <p>Relationships: {@link SorPolicy} carries this matrix beside the hot route
 * book so audit and future governance code can inspect the exact compiled
 * parameters that produced a published policy.</p>
 *
 * <p>Lifecycle: produced by policy compilation and treated as frozen once the
 * containing policy is published.</p>
 *
 * <p>Design intent: fields mirror the authoritative spec and are primitive
 * arrays to keep deterministic indexing and future binary persistence simple.</p>
 */
public final class FullPolicyMatrix {
    public final int instrumentCount;
    public final int venueCount;
    public final int regimeCount;
    public final int urgencyCount;

    public final boolean[] venueEligible;
    public final int[] venueRankScore;
    public final int[] venueWeightBps;
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
    public final int[] maxParticipationBps;
    public final short[] routeFlags;

    /**
     * Creates a full matrix whose arrays all use IVRU layout.
     */
    public FullPolicyMatrix(
            final int instrumentCount,
            final int venueCount,
            final int regimeCount,
            final int urgencyCount,
            final boolean[] venueEligible,
            final int[] venueRankScore,
            final int[] venueWeightBps,
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
            final int[] maxParticipationBps,
            final short[] routeFlags
    ) {
        this.instrumentCount = requirePositive("instrumentCount", instrumentCount);
        this.venueCount = requirePositive("venueCount", venueCount);
        this.regimeCount = requirePositive("regimeCount", regimeCount);
        this.urgencyCount = requirePositive("urgencyCount", urgencyCount);
        final int length = instrumentCount * venueCount * regimeCount * urgencyCount;
        this.venueEligible = requireBooleanArray("venueEligible", venueEligible, length);
        this.venueRankScore = requireIntArray("venueRankScore", venueRankScore, length);
        this.venueWeightBps = requireIntArray("venueWeightBps", venueWeightBps, length);
        this.latencyPenaltyNanos = requireIntArray("latencyPenaltyNanos", latencyPenaltyNanos, length);
        this.toxicityPenaltyBps = requireIntArray("toxicityPenaltyBps", toxicityPenaltyBps, length);
        this.fillProbabilityBps = requireIntArray("fillProbabilityBps", fillProbabilityBps, length);
        this.rejectPenaltyBps = requireIntArray("rejectPenaltyBps", rejectPenaltyBps, length);
        this.queueSurvivalBps = requireIntArray("queueSurvivalBps", queueSurvivalBps, length);
        this.feePenaltyTicks = requireIntArray("feePenaltyTicks", feePenaltyTicks, length);
        this.slippagePenaltyBps = requireIntArray("slippagePenaltyBps", slippagePenaltyBps, length);
        this.marketImpactPenaltyBps = requireIntArray("marketImpactPenaltyBps", marketImpactPenaltyBps, length);
        this.minChildQty = requireLongArray("minChildQty", minChildQty, length);
        this.maxChildQty = requireLongArray("maxChildQty", maxChildQty, length);
        this.maxParticipationBps = requireIntArray("maxParticipationBps", maxParticipationBps, length);
        this.routeFlags = requireShortArray("routeFlags", routeFlags, length);
    }

    /**
     * Computes the canonical IVRU index used by all matrix arrays.
     */
    public int idxIVRU(final int instrumentId, final int venueId, final int regimeId, final int urgencyId) {
        checkRange("instrumentId", instrumentId, instrumentCount);
        checkRange("venueId", venueId, venueCount);
        checkRange("regimeId", regimeId, regimeCount);
        checkRange("urgencyId", urgencyId, urgencyCount);
        return (((instrumentId * venueCount) + venueId) * regimeCount + regimeId) * urgencyCount + urgencyId;
    }

    private static int requirePositive(final String name, final int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static boolean[] requireBooleanArray(final String name, final boolean[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must equal instrumentCount * venueCount * regimeCount * urgencyCount");
        }
        return value;
    }

    private static int[] requireIntArray(final String name, final int[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must equal instrumentCount * venueCount * regimeCount * urgencyCount");
        }
        return value;
    }

    private static long[] requireLongArray(final String name, final long[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must equal instrumentCount * venueCount * regimeCount * urgencyCount");
        }
        return value;
    }

    private static short[] requireShortArray(final String name, final short[] value, final int length) {
        if (value == null || value.length != length) {
            throw new IllegalArgumentException(name + " length must equal instrumentCount * venueCount * regimeCount * urgencyCount");
        }
        return value;
    }

    private static void checkRange(final String name, final int value, final int upperExclusive) {
        if (value < 0 || value >= upperExclusive) {
            throw new IndexOutOfBoundsException(name + " out of range: " + value);
        }
    }
}
