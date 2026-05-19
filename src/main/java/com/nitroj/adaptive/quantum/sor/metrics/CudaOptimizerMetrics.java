package com.nitroj.adaptive.quantum.sor.metrics;

import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeOutput;

/**
 * Responsibility: aggregate CUDA tactical optimizer runtime measurements.
 *
 * <p>Role in system: Phase 2 completion reporting needs latency and native
 * status metrics for CUDA tactical cycles.</p>
 *
 * <p>Relationships: records {@link TacticalOptimizerNativeOutput} timings and
 * emits compact text reports for docs/tests.</p>
 *
 * <p>Lifecycle: created for a benchmark/reporting run and updated after each
 * optimizer cycle.</p>
 *
 * <p>Design intent: keep metrics dependency-free while preserving the
 * performance signal required by Phase 2.</p>
 */
public final class CudaOptimizerMetrics {
    private long runCount;
    private long totalRuntimeNanos;
    private long maxRuntimeNanos;
    private int lastStatusCode;

    /**
     * Records one native optimizer output.
     *
     * @param output native output with runtime diagnostics
     */
    public void record(final TacticalOptimizerNativeOutput output) {
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        runCount++;
        totalRuntimeNanos += output.nativeRuntimeNanos();
        maxRuntimeNanos = Math.max(maxRuntimeNanos, output.nativeRuntimeNanos());
        lastStatusCode = output.statusCode();
    }

    public long runCount() {
        return runCount;
    }

    public long averageRuntimeNanos() {
        return runCount == 0 ? 0L : totalRuntimeNanos / runCount;
    }

    public long maxRuntimeNanos() {
        return maxRuntimeNanos;
    }

    public int lastStatusCode() {
        return lastStatusCode;
    }

    /**
     * Renders the Phase 2 CUDA metrics summary.
     *
     * @return deterministic multi-line report
     */
    public String renderReport() {
        return "CUDA tactical optimizer metrics\n"
                + "runs=" + runCount + "\n"
                + "averageRuntimeNanos=" + averageRuntimeNanos() + "\n"
                + "maxRuntimeNanos=" + maxRuntimeNanos + "\n"
                + "lastStatusCode=" + lastStatusCode + "\n";
    }
}
