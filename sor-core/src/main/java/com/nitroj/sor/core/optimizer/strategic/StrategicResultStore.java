package com.nitroj.sor.core.optimizer.strategic;

import com.nitroj.sor.core.optimizer.OptimizerRunMetadata;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;

import java.util.Optional;

/**
 * Responsibility: retain the latest approved strategic venue subset result.
 *
 * <p>Role in system: CUDA tactical optimization consumes this approved state
 * rather than consuming the most recent attempted CUDA-Q strategic run.</p>
 *
 * <p>Relationships: written by {@link CudaQStrategicOptimizer} and read by
 * optimizer coordination, tests, and later audit/reporting code.</p>
 *
 * <p>Lifecycle: process-local Adaptive Quantum SOR store; later phases can replace it with a
 * durable or clustered implementation without changing the tactical contract.</p>
 *
 * <p>Design intent: version assignment is centralized so accepted results have
 * monotonic lineage while failed attempts preserve the prior active subset.</p>
 */
public final class StrategicResultStore {
    private StrategicVenueSubsetResult latestApproved;
    private OptimizerRunMetadata latestAttemptMetadata;
    private long nextVersion = 1L;

    public synchronized StrategicVenueSubsetResult approve(final StrategicVenueSubsetResult result, final OptimizerRunMetadata metadata) {
        if (result == null || metadata == null) {
            throw new IllegalArgumentException("result and metadata must not be null");
        }
        if (result.selectedVenueIds == null || result.selectedVenueIds.length == 0) {
            throw new IllegalArgumentException("approved strategic result must select at least one venue");
        }
        result.version = nextVersion++;
        latestApproved = result;
        latestAttemptMetadata = metadata;
        return result;
    }

    public synchronized void recordFailure(final OptimizerRunMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        latestAttemptMetadata = metadata;
    }

    public synchronized Optional<StrategicVenueSubsetResult> latestApproved() {
        return Optional.ofNullable(latestApproved);
    }

    public synchronized Optional<OptimizerRunMetadata> latestAttemptMetadata() {
        return Optional.ofNullable(latestAttemptMetadata);
    }
}
