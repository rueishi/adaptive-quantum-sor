package com.nitroj.adaptive.quantum.sor.metrics;

/**
 * Responsibility: produce static-vs-adaptive comparison reports.
 *
 * <p>Role in system: Phase 1 uses this runner to compare two routers on the
 * same scenario output and record realized improvement.</p>
 *
 * <p>Relationships: consumes {@link MetricsSnapshot} values from
 * {@link MetricsReporter} and emits {@link SorComparisonReport}.</p>
 *
 * <p>Lifecycle: stateless object used per scenario comparison.</p>
 *
 * <p>Design intent: small arithmetic-only comparison keeps reporting stable and
 * easy to validate.</p>
 */
public final class ComparisonRunner {
    /** Builds a comparison report from static and adaptive metrics. */
    public SorComparisonReport compare(
            final long scenarioId,
            final MetricsSnapshot staticMetrics,
            final MetricsSnapshot adaptiveMetrics
    ) {
        if (staticMetrics == null || adaptiveMetrics == null) {
            throw new IllegalArgumentException("metrics must not be null");
        }
        final SorComparisonReport report = new SorComparisonReport();
        report.scenarioId = scenarioId;
        report.staticSorRunId = scenarioId * 10L + 1L;
        report.adaptiveSorRunId = scenarioId * 10L + 2L;
        report.staticFillRateBps = staticMetrics.fillRateBps;
        report.adaptiveFillRateBps = adaptiveMetrics.fillRateBps;
        report.staticAvgSlippageBps = staticMetrics.avgSlippageBps;
        report.adaptiveAvgSlippageBps = adaptiveMetrics.avgSlippageBps;
        report.staticRejectRateBps = staticMetrics.rejectRateBps;
        report.adaptiveRejectRateBps = adaptiveMetrics.rejectRateBps;
        report.staticCompletionRateBps = staticMetrics.completionRateBps;
        report.adaptiveCompletionRateBps = adaptiveMetrics.completionRateBps;
        report.realizedImprovementBps = adaptiveMetrics.completionRateBps - staticMetrics.completionRateBps
                + staticMetrics.avgSlippageBps - adaptiveMetrics.avgSlippageBps;
        return report;
    }
}
