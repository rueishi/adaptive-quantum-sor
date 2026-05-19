package com.nitroj.adaptive.quantum.sor.api;

/**
 * Responsibility: expose current stats for notebook/API consumers.
 *
 * <p>Role in system: GET /stats/current returns this view.</p>
 *
 * <p>Relationships: populated from metrics and feature stats by the API server.</p>
 *
 * <p>Lifecycle: immutable-by-convention DTO for control-plane reads.</p>
 *
 * <p>Design intent: arrays match the spec's simple notebook stats panel.</p>
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
