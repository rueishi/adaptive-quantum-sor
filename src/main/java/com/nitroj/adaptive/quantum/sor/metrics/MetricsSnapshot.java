package com.nitroj.adaptive.quantum.sor.metrics;

/**
 * Responsibility: store bounded route metrics for one SOR run.
 *
 * <p>Role in system: comparison reporting consumes these snapshots for static
 * and adaptive runs.</p>
 *
 * <p>Relationships: produced by {@link MetricsReporter} and read by
 * {@link ComparisonRunner}.</p>
 *
 * <p>Lifecycle: immutable value object created after a route decision.</p>
 *
 * <p>Design intent: use bps values for rates so metrics remain deterministic.</p>
 */
public final class MetricsSnapshot {
    public final int fillRateBps;
    public final int completionRateBps;
    public final int avgSlippageBps;
    public final int rejectRateBps;
    public final int childOrderCount;
    public final long residualQty;

    public MetricsSnapshot(
            final int fillRateBps,
            final int completionRateBps,
            final int avgSlippageBps,
            final int rejectRateBps,
            final int childOrderCount,
            final long residualQty
    ) {
        this.fillRateBps = clamp(fillRateBps);
        this.completionRateBps = clamp(completionRateBps);
        this.avgSlippageBps = clamp(avgSlippageBps);
        this.rejectRateBps = clamp(rejectRateBps);
        this.childOrderCount = Math.max(0, childOrderCount);
        this.residualQty = Math.max(0L, residualQty);
    }

    private static int clamp(final int value) {
        return Math.max(0, Math.min(10_000, value));
    }
}
