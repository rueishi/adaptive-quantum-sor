package com.nitroj.adaptive.quantum.sor.optimizer.strategic;

import com.nitroj.adaptive.quantum.sor.nativebridge.StrategicOptimizerNativeBridge;
import com.nitroj.adaptive.quantum.sor.optimizer.OptimizerRunMetadata;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetOptimizer;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.ising.QuboObjectiveConfig;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

/**
 * Responsibility: produce approved strategic venue subsets from the CUDA-Q/QUBO backend.
 *
 * <p>Role in system: Phase 3 replaces direct use of the strategic Java stub
 * with a backend bridge while preserving the existing
 * {@link StrategicVenueSubsetOptimizer} contract consumed by the optimizer
 * coordinator.</p>
 *
 * <p>Relationships: builds {@link QuboObjectiveConfig}, calls
 * {@link StrategicOptimizerNativeBridge}, and publishes accepted output into
 * {@link StrategicResultStore}.</p>
 *
 * <p>Lifecycle: instantiated with a max subset size and reused across optimizer
 * cycles.</p>
 *
 * <p>Design intent: failed attempts are recorded as metadata but never replace
 * the latest approved result consumed by tactical optimization.</p>
 */
public final class CudaQStrategicOptimizer implements StrategicVenueSubsetOptimizer {
    public static final int OPTIMIZER_TYPE = 3;

    private final StrategicOptimizerNativeBridge bridge;
    private final StrategicResultStore store;
    private final int maxVenuesPerRoute;

    public CudaQStrategicOptimizer(
            final StrategicOptimizerNativeBridge bridge,
            final StrategicResultStore store,
            final int maxVenuesPerRoute
    ) {
        if (bridge == null || store == null) {
            throw new IllegalArgumentException("bridge and store must not be null");
        }
        if (maxVenuesPerRoute <= 0) {
            throw new IllegalArgumentException("maxVenuesPerRoute must be positive");
        }
        this.bridge = bridge;
        this.store = store;
        this.maxVenuesPerRoute = maxVenuesPerRoute;
    }

    @Override
    public StrategicVenueSubsetResult optimize(final PolicyOptimizationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        final OptimizerRunMetadata metadata = metadata(input);
        try {
            final StrategicVenueSubsetResult result = optimizeInternal(input, metadata);
            metadata.success = true;
            metadata.completedAtNanos = input.createdAtNanos;
            return store.approve(result, metadata);
        } catch (RuntimeException ex) {
            metadata.success = false;
            metadata.failureReason = ex.getMessage();
            metadata.completedAtNanos = input.createdAtNanos;
            store.recordFailure(metadata);
            throw ex;
        }
    }

    public StrategicResultStore store() {
        return store;
    }

    private StrategicVenueSubsetResult optimizeInternal(final PolicyOptimizationInput input, final OptimizerRunMetadata metadata) {
        final int routeCount = input.instrumentCount * input.regimeCount * input.urgencyCount;
        final StrategicVenueSubsetResult result = new StrategicVenueSubsetResult();
        result.createdAtNanos = input.createdAtNanos;
        result.instrumentCount = input.instrumentCount;
        result.regimeCount = input.regimeCount;
        result.urgencyCount = input.urgencyCount;
        result.subsetOffset = new int[routeCount + 1];
        result.selectedVenueIds = new short[routeCount * Math.min(maxVenuesPerRoute, input.venueCount)];
        result.optimizerRunId = metadata.optimizerRunId;
        result.optimizerType = OPTIMIZER_TYPE;

        int write = 0;
        for (int instrumentId = 0; instrumentId < input.instrumentCount; instrumentId++) {
            for (int regimeId = 0; regimeId < input.regimeCount; regimeId++) {
                for (int urgencyId = 0; urgencyId < input.urgencyCount; urgencyId++) {
                    final int routeKey = result.routeKey(instrumentId, regimeId, urgencyId);
                    result.subsetOffset[routeKey] = write;
                    final QuboObjectiveConfig objective = QuboObjectiveConfig.fromInput(
                            input, instrumentId, regimeId, urgencyId, maxVenuesPerRoute);
                    final StrategicOptimizerNativeBridge.NativeResult nativeResult = bridge.optimize(objective);
                    bridge.validate(nativeResult, input.venueCount);
                    for (final short venueId : nativeResult.selectedVenueIds()) {
                        result.selectedVenueIds[write++] = venueId;
                    }
                }
            }
        }
        result.subsetOffset[routeCount] = write;
        if (write < result.selectedVenueIds.length) {
            result.selectedVenueIds = java.util.Arrays.copyOf(result.selectedVenueIds, write);
        }
        return result;
    }

    private static OptimizerRunMetadata metadata(final PolicyOptimizationInput input) {
        final OptimizerRunMetadata metadata = new OptimizerRunMetadata();
        metadata.optimizerRunId = Math.max(1L, input.inputSnapshotId);
        metadata.optimizerType = OPTIMIZER_TYPE;
        metadata.startedAtNanos = input.createdAtNanos;
        metadata.inputSnapshotId = input.inputSnapshotId;
        metadata.marketDataSnapshotSeq = input.marketDataSnapshotSeq;
        metadata.venueStatsSnapshotSeq = input.venueStatsSnapshotSeq;
        metadata.modelSignalVersion = input.modelSignalVersion;
        metadata.currentPolicyVersion = input.currentPolicyVersion;
        return metadata;
    }
}
