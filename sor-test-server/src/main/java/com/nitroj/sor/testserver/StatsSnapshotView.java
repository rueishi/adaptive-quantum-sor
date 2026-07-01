package com.nitroj.sor.testserver;

/**
 * Responsibility: expose current stats for notebook/API consumers.
 *
 * <p>Role in system: {@code GET /stats/current} returns this view to notebooks
 * and demo dashboards. It represents the compact stats panel rather than the
 * full internal metric/state model.</p>
 *
 * <p>Relationships: populated by {@link NotebookScenarioHttpServer} from the
 * currently published policy and available demo/test statistics. It is consumed
 * by notebook helpers that convert responses into tables and charts.</p>
 *
 * <p>Lifecycle: created per API response and treated as immutable by
 * convention once serialized.</p>
 *
 * <p>Design intent: keep the notebook stats contract compact, dependency-free,
 * and stable while richer diagnostics live behind the supported control-plane
 * APIs.</p>
 */
public final class StatsSnapshotView {
    public long policyVersion;
    public int venueCount;
    public int[] venueWeights;
    public int[] latencyNanos;
    public int[] fillProbabilityBps;
    public int[] toxicityBps;

    public String toJson() {
        return "{\"policyVersion\":" + policyVersion + ",\"venueCount\":" + venueCount + "}";
    }
}
