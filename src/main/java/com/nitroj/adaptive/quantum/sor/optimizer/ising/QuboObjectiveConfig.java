package com.nitroj.adaptive.quantum.sor.optimizer.ising;

import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

import java.util.Arrays;

/**
 * Responsibility: define one route-key QUBO objective for strategic venue subset selection.
 *
 * <p>Role in system: Phase 3 strategic optimization converts model signals, venue stats,
 * risk limits, and metadata-derived eligibility into binary coefficients where
 * {@code x[v] = 1} means venue {@code v} is selected for the route.</p>
 *
 * <p>Relationships: consumed by the CUDA-Q/native strategic bridge and recorded
 * in strategic optimizer audit evidence.</p>
 *
 * <p>Lifecycle: built per instrument/regime/urgency route key and treated as
 * immutable after construction.</p>
 *
 * <p>Design intent: lower energy is better. Linear coefficients are negative
 * venue quality scores, while cardinality penalties make empty or over-large
 * subsets expensive without hiding their raw score contribution.</p>
 */
public final class QuboObjectiveConfig {
    public static final int DEFAULT_MIN_SUBSET_SIZE = 1;
    public static final int DEFAULT_PENALTY = 1_000_000;

    private final int instrumentId;
    private final int regimeId;
    private final int urgencyId;
    private final int venueCount;
    private final int minSubsetSize;
    private final int maxSubsetSize;
    private final int cardinalityPenalty;
    private final int[] linearCoefficients;
    private final int[] pairCoefficients;

    public QuboObjectiveConfig(
            final int instrumentId,
            final int regimeId,
            final int urgencyId,
            final int venueCount,
            final int minSubsetSize,
            final int maxSubsetSize,
            final int cardinalityPenalty,
            final int[] linearCoefficients,
            final int[] pairCoefficients
    ) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        if (minSubsetSize < 0 || minSubsetSize > venueCount) {
            throw new IllegalArgumentException("minSubsetSize must be in [0,venueCount]");
        }
        if (maxSubsetSize <= 0 || maxSubsetSize > venueCount || maxSubsetSize < minSubsetSize) {
            throw new IllegalArgumentException("maxSubsetSize must be in [minSubsetSize,venueCount]");
        }
        if (cardinalityPenalty <= 0) {
            throw new IllegalArgumentException("cardinalityPenalty must be positive");
        }
        if (linearCoefficients == null || linearCoefficients.length != venueCount) {
            throw new IllegalArgumentException("linearCoefficients length must equal venueCount");
        }
        final int pairLength = venueCount * venueCount;
        if (pairCoefficients == null || pairCoefficients.length != pairLength) {
            throw new IllegalArgumentException("pairCoefficients length must equal venueCount * venueCount");
        }
        this.instrumentId = instrumentId;
        this.regimeId = regimeId;
        this.urgencyId = urgencyId;
        this.venueCount = venueCount;
        this.minSubsetSize = minSubsetSize;
        this.maxSubsetSize = maxSubsetSize;
        this.cardinalityPenalty = cardinalityPenalty;
        this.linearCoefficients = Arrays.copyOf(linearCoefficients, linearCoefficients.length);
        this.pairCoefficients = Arrays.copyOf(pairCoefficients, pairCoefficients.length);
    }

    /**
     * Builds a one-route objective from the optimizer input snapshot.
     */
    public static QuboObjectiveConfig fromInput(
            final PolicyOptimizationInput input,
            final int instrumentId,
            final int regimeId,
            final int urgencyId,
            final int maxSubsetSize
    ) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (maxSubsetSize <= 0) {
            throw new IllegalArgumentException("maxSubsetSize must be positive");
        }
        final int boundedMax = Math.min(maxSubsetSize, input.venueCount);
        final int[] linear = new int[input.venueCount];
        final int[] pair = new int[input.venueCount * input.venueCount];
        for (int venueId = 0; venueId < input.venueCount; venueId++) {
            linear[venueId] = -venueQualityScore(input, instrumentId, venueId, regimeId);
        }
        return new QuboObjectiveConfig(
                instrumentId,
                regimeId,
                urgencyId,
                input.venueCount,
                DEFAULT_MIN_SUBSET_SIZE,
                boundedMax,
                DEFAULT_PENALTY,
                linear,
                pair
        );
    }

    private static int venueQualityScore(
            final PolicyOptimizationInput input,
            final int instrumentId,
            final int venueId,
            final int regimeId
    ) {
        int score = 5_000;
        if (input.modelSignals != null) {
            score += input.modelSignals.venueScoreBps(instrumentId, venueId, regimeId) - 5_000;
        }
        if (input.venueStats != null) {
            final int idx = input.venueStats.idxIVR(instrumentId, venueId, regimeId);
            score += input.venueStats.fillProbabilityBps[idx] / 10;
            score -= input.venueStats.toxicityBps[idx] / 10;
            score -= input.venueStats.rejectRateBps[idx] / 10;
            score -= input.venueStats.latencyNanos[idx] / 10_000;
            score -= input.venueStats.marketImpactBps[idx] / 10;
            score -= input.venueStats.feePenaltyTicks[idx];
        }
        if (input.riskLimits != null && input.riskLimits.maxParticipationBps(instrumentId, venueId) == 0) {
            score -= DEFAULT_PENALTY;
        }
        return score;
    }

    /**
     * Computes total QUBO energy for a candidate binary subset.
     */
    public long energy(final boolean[] selected) {
        if (selected == null || selected.length != venueCount) {
            throw new IllegalArgumentException("selected length must equal venueCount");
        }
        long energy = 0L;
        int count = 0;
        for (int i = 0; i < venueCount; i++) {
            if (selected[i]) {
                count++;
                energy += linearCoefficients[i];
            }
        }
        for (int i = 0; i < venueCount; i++) {
            if (!selected[i]) {
                continue;
            }
            for (int j = i + 1; j < venueCount; j++) {
                if (selected[j]) {
                    energy += pairCoefficient(i, j);
                }
            }
        }
        if (count < minSubsetSize) {
            final int missing = minSubsetSize - count;
            energy += (long) cardinalityPenalty * missing * missing;
        }
        if (count > maxSubsetSize) {
            final int excess = count - maxSubsetSize;
            energy += (long) cardinalityPenalty * excess * excess;
        }
        return energy;
    }

    public boolean validSubset(final boolean[] selected) {
        final int count = selectedCount(selected);
        return count >= minSubsetSize && count <= maxSubsetSize;
    }

    public int selectedCount(final boolean[] selected) {
        if (selected == null || selected.length != venueCount) {
            throw new IllegalArgumentException("selected length must equal venueCount");
        }
        int count = 0;
        for (final boolean value : selected) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    public int instrumentId() {
        return instrumentId;
    }

    public int regimeId() {
        return regimeId;
    }

    public int urgencyId() {
        return urgencyId;
    }

    public int venueCount() {
        return venueCount;
    }

    public int minSubsetSize() {
        return minSubsetSize;
    }

    public int maxSubsetSize() {
        return maxSubsetSize;
    }

    public int cardinalityPenalty() {
        return cardinalityPenalty;
    }

    public int linearCoefficient(final int venueId) {
        return linearCoefficients[venueId];
    }

    public int pairCoefficient(final int leftVenueId, final int rightVenueId) {
        return pairCoefficients[leftVenueId * venueCount + rightVenueId];
    }

    public int[] linearCoefficients() {
        return Arrays.copyOf(linearCoefficients, linearCoefficients.length);
    }

    public int[] pairCoefficients() {
        return Arrays.copyOf(pairCoefficients, pairCoefficients.length);
    }
}
