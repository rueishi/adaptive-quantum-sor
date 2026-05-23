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
 * venue quality scores, pair coefficients penalize toxic or concentration-prone
 * co-selection, and cardinality penalties make empty or over-large subsets
 * expensive without hiding their raw score contribution.</p>
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
            linear[venueId] = -clampBps(venueQualityScore(input, instrumentId, venueId, regimeId));
        }
        for (int leftVenueId = 0; leftVenueId < input.venueCount; leftVenueId++) {
            for (int rightVenueId = leftVenueId + 1; rightVenueId < input.venueCount; rightVenueId++) {
                final int penalty = pairPenalty(input, instrumentId, leftVenueId, rightVenueId, regimeId);
                pair[leftVenueId * input.venueCount + rightVenueId] = penalty;
                pair[rightVenueId * input.venueCount + leftVenueId] = penalty;
            }
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
        if (input.venueMetadata != null
                && (!input.venueMetadata.isEnabled(venueId) || !input.venueMetadata.supportsInstrument(instrumentId, venueId))) {
            score -= DEFAULT_PENALTY;
        }
        return score;
    }

    private static int pairPenalty(
            final PolicyOptimizationInput input,
            final int instrumentId,
            final int leftVenueId,
            final int rightVenueId,
            final int regimeId
    ) {
        if (input.venueStats == null) {
            return 0;
        }
        final int leftIdx = input.venueStats.idxIVR(instrumentId, leftVenueId, regimeId);
        final int rightIdx = input.venueStats.idxIVR(instrumentId, rightVenueId, regimeId);

        int penalty = 0;
        penalty += sharedRiskPenalty(input.venueStats.toxicityBps[leftIdx], input.venueStats.toxicityBps[rightIdx], 1_000, 5, 1_500);
        penalty += sharedRiskPenalty(input.venueStats.rejectRateBps[leftIdx], input.venueStats.rejectRateBps[rightIdx], 1_000, 5, 1_500);
        penalty += sharedRiskPenalty(input.venueStats.marketImpactBps[leftIdx], input.venueStats.marketImpactBps[rightIdx], 1_000, 5, 1_000);

        final int profileDistance =
                Math.abs(input.venueStats.toxicityBps[leftIdx] - input.venueStats.toxicityBps[rightIdx]) / 10
                        + Math.abs(input.venueStats.rejectRateBps[leftIdx] - input.venueStats.rejectRateBps[rightIdx]) / 10
                        + Math.abs(input.venueStats.marketImpactBps[leftIdx] - input.venueStats.marketImpactBps[rightIdx]) / 10
                        + Math.abs(input.venueStats.fillProbabilityBps[leftIdx] - input.venueStats.fillProbabilityBps[rightIdx]) / 20
                        + Math.abs(input.venueStats.latencyNanos[leftIdx] - input.venueStats.latencyNanos[rightIdx]) / 100_000;
        penalty += Math.max(0, 1_000 - Math.min(1_000, profileDistance)) / 2;
        return Math.min(DEFAULT_PENALTY, penalty);
    }

    private static int sharedRiskPenalty(
            final int left,
            final int right,
            final int threshold,
            final int divisor,
            final int cap
    ) {
        if (left < threshold || right < threshold) {
            return 0;
        }
        return Math.min(cap, ((left + right) / 2) / divisor);
    }

    private static int clampBps(final int value) {
        return Math.max(0, Math.min(10_000, value));
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
