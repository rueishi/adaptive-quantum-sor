package com.nitroj.sor.optnative;

/**
 * Responsibility: expose current CUDA tactical backend health.
 *
 * <p>Role in system: fallback and reporting paths inspect this value after
 * native optimization attempts.</p>
 *
 * <p>Relationships: populated by {@link com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizer}
 * from {@link TacticalOptimizerNativeOutput} status codes.</p>
 *
 * <p>Lifecycle: immutable snapshot created per optimizer run.</p>
 *
 * <p>Design intent: make GPU/library/timeout state explicit instead of burying
 * it inside exception strings.</p>
 */
public record CudaOptimizerHealth(
        boolean libraryLoaded,
        boolean gpuAvailable,
        boolean timedOut,
        int lastStatusCode,
        String message
) {
    public CudaOptimizerHealth {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    /**
     * Returns whether CUDA output is healthy enough to consume directly.
     *
     * @return true when library and GPU are available, no timeout occurred, and
     * status is OK
     */
    public boolean healthy() {
        return libraryLoaded && gpuAvailable && !timedOut && TacticalOptimizerNativeStatus.ok(lastStatusCode);
    }
}
