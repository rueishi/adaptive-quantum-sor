package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

/**
 * Responsibility: provide deterministic strategic venue subset selection.
 *
 * <p>Role in system: this Phase 1 Java stub stands in for the future
 * CUDA-Q/Ising strategic optimizer and chooses a bounded set of venues per
 * route key.</p>
 *
 * <p>Relationships: reads {@link PolicyOptimizationInput} stats/model signals
 * and writes {@link StrategicVenueSubsetResult} for tactical optimization and
 * policy compilation.</p>
 *
 * <p>Lifecycle: instantiated by warm-path optimizer coordination and reused
 * across optimizer cycles.</p>
 *
 * <p>Design intent: deterministic sorting by model/stat score gives stable,
 * testable behavior without introducing native dependencies in Phase 1.</p>
 */
public final class IsingCudaQStrategicOptimizerStub implements StrategicVenueSubsetOptimizer {
    public static final int OPTIMIZER_TYPE = 1;
    private final int maxVenuesPerRoute;
    private final boolean simulateFailure;

    public IsingCudaQStrategicOptimizerStub(final int maxVenuesPerRoute) {
        this(maxVenuesPerRoute, false);
    }

    public IsingCudaQStrategicOptimizerStub(final int maxVenuesPerRoute, final boolean simulateFailure) {
        if (maxVenuesPerRoute <= 0) {
            throw new IllegalArgumentException("maxVenuesPerRoute must be positive");
        }
        this.maxVenuesPerRoute = maxVenuesPerRoute;
        this.simulateFailure = simulateFailure;
    }

    /**
     * Selects top-scoring venues for every route key. If inputs are sparse or
     * stats are unavailable, the dense venue order is still valid and stable.
     */
    @Override
    public StrategicVenueSubsetResult optimize(final PolicyOptimizationInput input) {
        if (simulateFailure) {
            throw new IllegalStateException("strategic optimizer failure simulated");
        }
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        final int routeCount = input.instrumentCount * input.regimeCount * input.urgencyCount;
        final int selectedPerRoute = Math.min(maxVenuesPerRoute, input.venueCount);
        final StrategicVenueSubsetResult result = new StrategicVenueSubsetResult();
        result.version = Math.max(1L, input.inputSnapshotId);
        result.createdAtNanos = input.createdAtNanos;
        result.instrumentCount = input.instrumentCount;
        result.regimeCount = input.regimeCount;
        result.urgencyCount = input.urgencyCount;
        result.subsetOffset = new int[routeCount + 1];
        result.selectedVenueIds = new short[routeCount * selectedPerRoute];
        result.optimizerRunId = input.inputSnapshotId;
        result.optimizerType = OPTIMIZER_TYPE;

        int write = 0;
        for (int instrumentId = 0; instrumentId < input.instrumentCount; instrumentId++) {
            for (int regimeId = 0; regimeId < input.regimeCount; regimeId++) {
                for (int urgencyId = 0; urgencyId < input.urgencyCount; urgencyId++) {
                    final int routeKey = result.routeKey(instrumentId, regimeId, urgencyId);
                    result.subsetOffset[routeKey] = write;
                    final boolean[] chosen = new boolean[input.venueCount];
                    for (int rank = 0; rank < selectedPerRoute; rank++) {
                        final int venueId = bestVenue(input, instrumentId, regimeId, chosen);
                        chosen[venueId] = true;
                        result.selectedVenueIds[write++] = (short) venueId;
                    }
                }
            }
        }
        result.subsetOffset[routeCount] = write;
        return result;
    }

    private static int bestVenue(
            final PolicyOptimizationInput input,
            final int instrumentId,
            final int regimeId,
            final boolean[] chosen
    ) {
        int bestVenue = 0;
        int bestScore = Integer.MIN_VALUE;
        for (int venueId = 0; venueId < input.venueCount; venueId++) {
            if (chosen[venueId]) {
                continue;
            }
            final int score = score(input, instrumentId, venueId, regimeId);
            if (score > bestScore || (score == bestScore && venueId < bestVenue)) {
                bestScore = score;
                bestVenue = venueId;
            }
        }
        return bestVenue;
    }

    private static int score(final PolicyOptimizationInput input, final int instrumentId, final int venueId, final int regimeId) {
        int score = 5_000;
        if (input.modelSignals != null) {
            score += input.modelSignals.venueScoreBps(instrumentId, venueId, regimeId) - 5_000;
        }
        if (input.venueStats != null) {
            final int idx = input.venueStats.idxIVR(instrumentId, venueId, regimeId);
            score += input.venueStats.fillProbabilityBps[idx] / 10;
            score -= input.venueStats.toxicityBps[idx] / 10;
            score -= input.venueStats.rejectRateBps[idx] / 10;
        }
        return score;
    }
}
