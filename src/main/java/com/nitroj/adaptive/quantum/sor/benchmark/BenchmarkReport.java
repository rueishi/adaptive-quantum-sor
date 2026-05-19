package com.nitroj.adaptive.quantum.sor.benchmark;

/**
 * Responsibility: carry benchmark latency/allocation results.
 *
 * <p>Role in system: tests and manual benchmark scripts inspect this report for
 * regression threshold support.</p>
 *
 * <p>Relationships: returned by benchmark harness classes.</p>
 *
 * <p>Lifecycle: immutable after construction.</p>
 *
 * <p>Design intent: keep benchmark output structured and dependency-free.</p>
 */
public record BenchmarkReport(int iterations, long averageLatencyNanos, long allocationBytes, long latencyThresholdNanos) {
    public BenchmarkReport {
        if (iterations <= 0 || averageLatencyNanos < 0 || allocationBytes < 0 || latencyThresholdNanos <= 0) {
            throw new IllegalArgumentException("benchmark report values must be valid");
        }
    }

    /** Returns true when the measured average exceeds the configured threshold. */
    public boolean latencyRegression() {
        return averageLatencyNanos > latencyThresholdNanos;
    }
}
