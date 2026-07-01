package com.nitroj.sor.core.metrics;

/**
 * Responsibility: compare static and adaptive SOR route metrics.
 *
 * <p>Role in system: this report is the main Phase 1 output for showing
 * realized improvement or underperformance.</p>
 *
 * <p>Relationships: produced by {@link ComparisonRunner} from two
 * {@link MetricsSnapshot} instances.</p>
 *
 * <p>Lifecycle: immutable by convention after fields are populated.</p>
 *
 * <p>Design intent: mirrors the spec's simple report structure for later API
 * and notebook views.</p>
 */
public final class SorComparisonReport {
    public long scenarioId;
    public long staticSorRunId;
    public long adaptiveSorRunId;
    public int staticFillRateBps;
    public int adaptiveFillRateBps;
    public int staticAvgSlippageBps;
    public int adaptiveAvgSlippageBps;
    public int staticRejectRateBps;
    public int adaptiveRejectRateBps;
    public int staticCompletionRateBps;
    public int adaptiveCompletionRateBps;
    public int realizedImprovementBps;
}
