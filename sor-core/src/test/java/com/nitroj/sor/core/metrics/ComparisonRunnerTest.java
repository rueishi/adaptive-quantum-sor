package com.nitroj.sor.core.metrics;

import com.nitroj.sor.core.execution.RouteDecisionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify comparison metrics and reports.
 *
 * <p>Role in system: covers P1-TC-020 comparison report generation, zero
 * improvement, adaptive underperformance, and bounded metrics.</p>
 *
 * <p>Relationships: uses {@link MetricsReporter} output and
 * {@link ComparisonRunner} report aggregation.</p>
 *
 * <p>Lifecycle: executed by Gradle as metrics coverage.</p>
 *
 * <p>Design intent: keep comparison arithmetic simple and transparent.</p>
 */
final class ComparisonRunnerTest {
    @Test
    void identicalBehaviorReportsZeroImprovement() {
        final MetricsSnapshot metrics = new MetricsSnapshot(10_000, 10_000, 0, 0, 1, 0);
        final SorComparisonReport report = new ComparisonRunner().compare(1L, metrics, metrics);

        assertEquals(0, report.realizedImprovementBps);
        assertEquals(10_000, report.staticFillRateBps);
        assertEquals(10_000, report.adaptiveFillRateBps);
    }

    @Test
    void adaptiveUnderperformanceReportsNegativeImprovement() {
        final SorComparisonReport report = new ComparisonRunner().compare(2L,
                new MetricsSnapshot(10_000, 10_000, 0, 0, 1, 0),
                new MetricsSnapshot(5_000, 5_000, 0, 0, 1, 50));

        assertTrue(report.realizedImprovementBps < 0);
    }

    @Test
    void metricsBoundedAndReporterCalculatesRates() {
        final MetricsSnapshot bounded = new MetricsSnapshot(20_000, -1, 20_000, -5, -1, -1);
        final MetricsSnapshot reported = new MetricsReporter().snapshot(
                new RouteDecisionResult(1L, 1L, 1L, 1, 50L, 50L, 1, null), 100L);

        assertEquals(10_000, bounded.fillRateBps);
        assertEquals(0, bounded.completionRateBps);
        assertEquals(5_000, reported.fillRateBps);
        assertEquals(5_000, reported.completionRateBps);
        assertThrows(IllegalArgumentException.class, () -> new ComparisonRunner().compare(1L, null, bounded));
        assertThrows(IllegalArgumentException.class, () -> new MetricsReporter().snapshot(null, 1L));
    }
}
