package com.nitroj.sor.testkit.policy.robust;

import com.nitroj.sor.core.policy.FullPolicyMatrix;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.testkit.scenario.ScenarioSummary;

/**
 * Responsibility: deterministic Phase 7 scorecard over existing scenario summaries.
 */
public final class ScenarioScorecardV1 {
    public static final String VERSION = "ScenarioScorecardV1";

    public long score(final ScenarioSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("summary must not be null");
        }
        return summary.fullFillCount() * 10_000L
                + summary.partialFillCount() * 2_500L
                - summary.rejectCount() * 5_000L
                - summary.residualQty()
                - summary.staleEventCount() * 250L
                - summary.outageEventCount() * 1_000L;
    }

    public long score(final ScenarioSummary summary, final SorPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        return score(summary) + policyScenarioAdjustment(summary, policy.fullPolicyMatrix);
    }

    private static long policyScenarioAdjustment(final ScenarioSummary summary, final FullPolicyMatrix matrix) {
        final long[] regimeWeights = regimeWeights(summary, matrix.regimeCount);
        long weightedSum = 0L;
        long totalWeight = 0L;
        for (int instrumentId = 0; instrumentId < matrix.instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < matrix.venueCount; venueId++) {
                for (int regimeId = 0; regimeId < matrix.regimeCount; regimeId++) {
                    for (int urgencyId = 0; urgencyId < matrix.urgencyCount; urgencyId++) {
                        final int idx = matrix.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        if (!matrix.venueEligible[idx]) {
                            continue;
                        }
                        final long weight = regimeWeights[regimeId] * (urgencyId + 1L);
                        weightedSum += cellQuality(matrix, idx) * weight;
                        totalWeight += weight;
                    }
                }
            }
        }
        return totalWeight == 0L ? 0L : weightedSum / totalWeight;
    }

    private static long cellQuality(final FullPolicyMatrix matrix, final int idx) {
        return matrix.fillProbabilityBps[idx]
                + matrix.queueSurvivalBps[idx] / 2L
                + matrix.venueWeightBps[idx] / 4L
                + matrix.maxParticipationBps[idx] / 20L
                - matrix.toxicityPenaltyBps[idx]
                - matrix.rejectPenaltyBps[idx]
                - matrix.slippagePenaltyBps[idx] / 2L
                - matrix.marketImpactPenaltyBps[idx] / 2L
                - matrix.latencyPenaltyNanos[idx] / 1_000L;
    }

    private static long[] regimeWeights(final ScenarioSummary summary, final int regimeCount) {
        final long[] weights = new long[regimeCount];
        if (regimeCount > 0) {
            weights[0] = Math.max(0, summary.detectedNormalCount());
        }
        if (regimeCount > 1) {
            weights[1] = Math.max(0, summary.detectedVolatileCount());
        }
        if (regimeCount > 2) {
            weights[2] = Math.max(0, summary.detectedThinBookCount());
        }
        long total = 0L;
        for (long weight : weights) {
            total += weight;
        }
        if (total == 0L) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1L;
            }
        }
        return weights;
    }
}
