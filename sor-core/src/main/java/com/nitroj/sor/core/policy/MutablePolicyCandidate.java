package com.nitroj.sor.core.policy;

/**
 * Responsibility: hold warm-path policy parameters while optimizers collaborate.
 *
 * <p>Role in system: deterministic optimizer stubs write candidate IVRU arrays
 * here before later linting and compilation turn them into immutable published
 * policy structures.</p>
 *
 * <p>Relationships: consumes strategic and tactical optimizer output and is the
 * mutable predecessor of {@link FullPolicyMatrix} and {@link HotRouteBook}.</p>
 *
 * <p>Lifecycle: created per optimizer cycle, mutated only before compilation,
 * and discarded after the candidate is accepted or rejected.</p>
 *
 * <p>Design intent: this class intentionally exposes arrays for simple Phase 1
 * optimizer stubs while centralizing dimension validation.</p>
 */
public final class MutablePolicyCandidate {
    public final int instrumentCount;
    public final int venueCount;
    public final int regimeCount;
    public final int urgencyCount;

    public boolean[] venueEligible;
    public int[] venueWeightBps;
    public int[] latencyPenaltyNanos;
    public int[] toxicityPenaltyBps;
    public int[] fillProbabilityBps;
    public int[] rejectPenaltyBps;
    public long[] minChildQty;
    public long[] maxChildQty;
    public int[] maxParticipationBps;
    public short[] routeFlags;

    /**
     * Allocates empty candidate arrays in canonical IVRU layout.
     */
    public MutablePolicyCandidate(
            final int instrumentCount,
            final int venueCount,
            final int regimeCount,
            final int urgencyCount
    ) {
        this.instrumentCount = requirePositive("instrumentCount", instrumentCount);
        this.venueCount = requirePositive("venueCount", venueCount);
        this.regimeCount = requirePositive("regimeCount", regimeCount);
        this.urgencyCount = requirePositive("urgencyCount", urgencyCount);
        final int length = instrumentCount * venueCount * regimeCount * urgencyCount;
        this.venueEligible = new boolean[length];
        this.venueWeightBps = new int[length];
        this.latencyPenaltyNanos = new int[length];
        this.toxicityPenaltyBps = new int[length];
        this.fillProbabilityBps = new int[length];
        this.rejectPenaltyBps = new int[length];
        this.minChildQty = new long[length];
        this.maxChildQty = new long[length];
        this.maxParticipationBps = new int[length];
        this.routeFlags = new short[length];
    }

    /**
     * Computes the canonical IVRU index for candidate updates.
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

    private static void checkRange(final String name, final int value, final int upperExclusive) {
        if (value < 0 || value >= upperExclusive) {
            throw new IndexOutOfBoundsException(name + " out of range: " + value);
        }
    }
}
