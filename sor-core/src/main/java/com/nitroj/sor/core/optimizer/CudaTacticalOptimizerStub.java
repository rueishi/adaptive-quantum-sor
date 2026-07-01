package com.nitroj.sor.core.optimizer;

import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;

/**
 * Responsibility: provide deterministic tactical policy tuning.
 *
 * <p>Role in system: this Phase 1 Java stub stands in for the future CUDA/cuOpt
 * tactical optimizer and produces bounded IVRU arrays.</p>
 *
 * <p>Relationships: consumes {@link StrategicVenueSubsetResult} and
 * {@link PolicyOptimizationInput}; writes {@link TacticalPolicyResult} and can
 * apply its output to {@link MutablePolicyCandidate}.</p>
 *
 * <p>Lifecycle: called after strategic selection during warm-path optimizer
 * cycles, never from route execution.</p>
 *
 * <p>Design intent: deterministic formulas keep tests stable while exercising
 * the same array surfaces later native optimization will populate.</p>
 */
public final class CudaTacticalOptimizerStub implements TacticalPolicyOptimizer {
    public static final int OPTIMIZER_TYPE = 2;
    private final boolean simulateFailure;

    public CudaTacticalOptimizerStub() {
        this(false);
    }

    public CudaTacticalOptimizerStub(final boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    /**
     * Produces bounded tactical values for every IVRU cell.
     */
    @Override
    public TacticalPolicyResult optimize(final StrategicVenueSubsetResult subset, final PolicyOptimizationInput input) {
        if (simulateFailure) {
            throw new IllegalStateException("tactical optimizer failure simulated");
        }
        if (subset == null || input == null) {
            throw new IllegalArgumentException("subset and input must not be null");
        }
        final int length = input.instrumentCount * input.venueCount * input.regimeCount * input.urgencyCount;
        final TacticalPolicyResult result = new TacticalPolicyResult();
        result.version = Math.max(1L, input.inputSnapshotId);
        result.createdAtNanos = input.createdAtNanos;
        result.strategicSubsetVersion = subset.version;
        result.venueWeightBps = new int[length];
        result.latencyPenaltyNanos = new int[length];
        result.toxicityPenaltyBps = new int[length];
        result.fillProbabilityBps = new int[length];
        result.rejectPenaltyBps = new int[length];
        result.queueSurvivalBps = new int[length];
        result.slippagePenaltyBps = new int[length];
        result.marketImpactPenaltyBps = new int[length];
        result.minChildQty = new long[length];
        result.maxChildQty = new long[length];
        result.maxParticipationBps = new int[length];

        for (int instrumentId = 0; instrumentId < input.instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < input.venueCount; venueId++) {
                for (int regimeId = 0; regimeId < input.regimeCount; regimeId++) {
                    for (int urgencyId = 0; urgencyId < input.urgencyCount; urgencyId++) {
                        final int idx = input.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        final int ivr = input.venueStats == null ? -1 : input.venueStats.idxIVR(instrumentId, venueId, regimeId);
                        result.fillProbabilityBps[idx] = ivr < 0 ? 5_000 : clampBps(input.venueStats.fillProbabilityBps[ivr]);
                        result.toxicityPenaltyBps[idx] = ivr < 0 ? 0 : clampBps(input.venueStats.toxicityBps[ivr]);
                        result.rejectPenaltyBps[idx] = ivr < 0 ? 0 : clampBps(input.venueStats.rejectRateBps[ivr]);
                        result.latencyPenaltyNanos[idx] = ivr < 0 ? 0 : Math.max(0, input.venueStats.latencyNanos[ivr]);
                        result.venueWeightBps[idx] = subsetContains(subset, input, instrumentId, venueId, regimeId, urgencyId) ? 1_000 : 0;
                        result.queueSurvivalBps[idx] = 5_000;
                        result.slippagePenaltyBps[idx] = ivr < 0 ? 0 : clampBps(input.venueStats.marketImpactBps[ivr]);
                        result.marketImpactPenaltyBps[idx] = result.slippagePenaltyBps[idx];
                        result.minChildQty[idx] = 1L;
                        result.maxChildQty[idx] = input.riskLimits == null ? 10_000L : Math.max(1L, input.riskLimits.maxChildQty(instrumentId));
                        result.maxParticipationBps[idx] = input.riskLimits == null
                                ? 2_500
                                : clampBps(input.riskLimits.maxParticipationBps(instrumentId, venueId));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Applies tactical output and strategic eligibility to a mutable candidate.
     */
    public void applyToCandidate(
            final MutablePolicyCandidate candidate,
            final StrategicVenueSubsetResult subset,
            final PolicyOptimizationInput input,
            final TacticalPolicyResult result
    ) {
        if (candidate == null || subset == null || input == null || result == null) {
            throw new IllegalArgumentException("candidate, subset, input, and result must not be null");
        }
        for (int instrumentId = 0; instrumentId < input.instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < input.venueCount; venueId++) {
                for (int regimeId = 0; regimeId < input.regimeCount; regimeId++) {
                    for (int urgencyId = 0; urgencyId < input.urgencyCount; urgencyId++) {
                        final int idx = candidate.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        candidate.venueEligible[idx] = subsetContains(subset, input, instrumentId, venueId, regimeId, urgencyId);
                        candidate.venueWeightBps[idx] = result.venueWeightBps[idx];
                        candidate.latencyPenaltyNanos[idx] = result.latencyPenaltyNanos[idx];
                        candidate.toxicityPenaltyBps[idx] = result.toxicityPenaltyBps[idx];
                        candidate.fillProbabilityBps[idx] = result.fillProbabilityBps[idx];
                        candidate.rejectPenaltyBps[idx] = result.rejectPenaltyBps[idx];
                        candidate.minChildQty[idx] = result.minChildQty[idx];
                        candidate.maxChildQty[idx] = result.maxChildQty[idx];
                        candidate.maxParticipationBps[idx] = result.maxParticipationBps[idx];
                    }
                }
            }
        }
    }

    private static boolean subsetContains(
            final StrategicVenueSubsetResult subset,
            final PolicyOptimizationInput input,
            final int instrumentId,
            final int venueId,
            final int regimeId,
            final int urgencyId
    ) {
        final int routeKey = input.routeKey(instrumentId, regimeId, urgencyId);
        for (int i = subset.subsetOffset[routeKey]; i < subset.subsetOffset[routeKey + 1]; i++) {
            if (subset.selectedVenueIds[i] == venueId) {
                return true;
            }
        }
        return false;
    }

    private static int clampBps(final int value) {
        return Math.max(0, Math.min(10_000, value));
    }
}
