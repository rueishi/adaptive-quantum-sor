package com.nitroj.adaptive.quantum.sor.benchmark;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Responsibility: verify benchmark harness contracts for Phase 1 task cards.
 *
 * <p>Role in system: benchmark classes are intended for manual/CI invocation,
 * but tests keep the reporting shape and regression threshold support stable.</p>
 *
 * <p>Relationships: covers both static and policy-driven benchmark entry
 * points.</p>
 *
 * <p>Lifecycle: executed as part of the unit test suite.</p>
 *
 * <p>Design intent: keep the benchmark harness dependency-free while still
 * proving latency, allocation, and threshold outputs are available.</p>
 */
class BenchmarkHarnessTest {
    @Test
    void staticBenchmarkRunsAndReportsLatencyAndAllocation() {
        final BenchmarkReport report = new StaticSorBenchmark().run(10);

        assertEquals(10, report.iterations());
        assertTrue(report.averageLatencyNanos() >= 0);
        assertTrue(report.allocationBytes() >= 0);
        assertFalse(report.latencyRegression() && report.averageLatencyNanos() <= report.latencyThresholdNanos());
    }

    @Test
    void policyDrivenBenchmarkRunsAndReportsLatencyAndAllocation() {
        final BenchmarkReport report = new PolicyDrivenSorBenchmark().run(10);

        assertEquals(10, report.iterations());
        assertTrue(report.averageLatencyNanos() >= 0);
        assertTrue(report.allocationBytes() >= 0);
        assertTrue(report.latencyThresholdNanos() > 0);
    }

    @Test
    void regressionThresholdIsSupportedAndInputsAreValidated() {
        assertTrue(new BenchmarkReport(1, 3_000L, 0L, 2_000L).latencyRegression());
        assertFalse(new BenchmarkReport(1, 1_000L, 0L, 2_000L).latencyRegression());

        assertDoesNotThrow(() -> new BenchmarkReport(1, 0L, 0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkReport(0, 1L, 0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> new StaticSorBenchmark().run(0));
    }
}
