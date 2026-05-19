package com.nitroj.adaptive.quantum.sor.optimizer.ising;

import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

/**
 * Responsibility: choose safe fallback behavior for CUDA-Q strategic failures.
 *
 * <p>Role in system: when the native strategic backend is unavailable, times
 * out, or returns invalid output, this policy decides whether to reuse a prior
 * approved strategic result, invoke the deterministic stub, or fail closed.</p>
 *
 * <p>Relationships: used with {@link CudaQOptimizerHealth} and the Phase 3
 * strategic optimizer store.</p>
 *
 * <p>Lifecycle: immutable configuration created at optimizer startup.</p>
 *
 * <p>Design intent: fail-safe choices are explicit so tests and reports can
 * prove the current policy is not silently replaced by bad strategic output.</p>
 */
public final class CudaQFallbackPolicy {
    private final boolean fallbackToPreviousApproved;
    private final boolean fallbackToStub;
    private final int stubMaxVenuesPerRoute;

    public CudaQFallbackPolicy(
            final boolean fallbackToPreviousApproved,
            final boolean fallbackToStub,
            final int stubMaxVenuesPerRoute
    ) {
        if (stubMaxVenuesPerRoute <= 0) {
            throw new IllegalArgumentException("stubMaxVenuesPerRoute must be positive");
        }
        this.fallbackToPreviousApproved = fallbackToPreviousApproved;
        this.fallbackToStub = fallbackToStub;
        this.stubMaxVenuesPerRoute = stubMaxVenuesPerRoute;
    }

    public static CudaQFallbackPolicy previousThenStub(final int stubMaxVenuesPerRoute) {
        return new CudaQFallbackPolicy(true, true, stubMaxVenuesPerRoute);
    }

    public static CudaQFallbackPolicy failClosed() {
        return new CudaQFallbackPolicy(false, false, 1);
    }

    public StrategicVenueSubsetResult fallback(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult previousApproved,
            final CudaQOptimizerHealth health
    ) {
        if (input == null || health == null) {
            throw new IllegalArgumentException("input and health must not be null");
        }
        if (fallbackToPreviousApproved && previousApproved != null) {
            health.recordFallback("CUDA-Q fallback reused previous strategic result");
            return previousApproved;
        }
        if (fallbackToStub) {
            health.recordFallback("CUDA-Q fallback used strategic stub");
            return new IsingCudaQStrategicOptimizerStub(stubMaxVenuesPerRoute).optimize(input);
        }
        health.recordFailure("CUDA-Q fallback disabled");
        throw new IllegalStateException("CUDA-Q fallback disabled");
    }
}
