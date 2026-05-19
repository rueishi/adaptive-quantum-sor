package com.nitroj.adaptive.quantum.sor.metrics;

import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;

/**
 * Responsibility: convert route decisions into bounded metrics snapshots.
 *
 * <p>Role in system: comparison and API layers use this reporter to avoid
 * duplicating fill/completion rate calculations.</p>
 *
 * <p>Relationships: reads {@link RouteDecisionResult} and produces
 * {@link MetricsSnapshot}.</p>
 *
 * <p>Lifecycle: stateless utility object reused across comparison runs.</p>
 *
 * <p>Design intent: keep Phase 1 metrics simple and deterministic.</p>
 */
public final class MetricsReporter {
    /** Produces metrics for a route result and requested parent quantity. */
    public MetricsSnapshot snapshot(final RouteDecisionResult result, final long parentQty) {
        if (result == null || parentQty <= 0) {
            throw new IllegalArgumentException("result must not be null and parentQty must be positive");
        }
        final int fillRate = (int) Math.min(10_000L, result.routedQty * 10_000L / parentQty);
        final int completion = result.residualQty == 0 ? 10_000 : fillRate;
        return new MetricsSnapshot(fillRate, completion, 0, 0, result.childOrderCount, result.residualQty);
    }
}
