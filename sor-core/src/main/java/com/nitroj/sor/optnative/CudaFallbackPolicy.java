package com.nitroj.sor.optnative;

/**
 * Responsibility: configure safe fallback behavior for CUDA tactical failures.
 *
 * <p>Role in system: the native optimizer wrapper uses this policy to decide
 * whether library/GPU/timeout/native errors should fall back to the Java stub or
 * fail the optimizer cycle.</p>
 *
 * <p>Relationships: consumed by {@link com.nitroj.sor.core.optimizer.CudaTacticalOptimizer}.</p>
 *
 * <p>Lifecycle: immutable runtime configuration created during optimizer
 * wiring.</p>
 *
 * <p>Design intent: keep Phase 2 safety behavior explicit and testable without
 * hard-coding fallback decisions inside the bridge.</p>
 */
public record CudaFallbackPolicy(boolean fallbackEnabled, long timeoutNanos) {
    public CudaFallbackPolicy {
        if (timeoutNanos <= 0) {
            throw new IllegalArgumentException("timeoutNanos must be positive");
        }
    }

    public static CudaFallbackPolicy enabled(final long timeoutNanos) {
        return new CudaFallbackPolicy(true, timeoutNanos);
    }

    public static CudaFallbackPolicy disabled(final long timeoutNanos) {
        return new CudaFallbackPolicy(false, timeoutNanos);
    }
}
