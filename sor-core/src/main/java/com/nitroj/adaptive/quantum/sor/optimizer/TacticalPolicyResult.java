package com.nitroj.adaptive.quantum.sor.optimizer;

/**
 * Responsibility: carry tactical optimizer numeric policy parameters.
 *
 * <p>Role in system: the L3 optimizer tunes weights, penalties, child-size
 * limits, and participation caps after strategic venue selection has bounded
 * the route universe.</p>
 *
 * <p>Relationships: policy candidates and compilers consume these arrays using
 * the same IVRU layout as other policy structures.</p>
 *
 * <p>Lifecycle: produced per tactical optimizer run and treated as frozen after
 * the run is recorded.</p>
 *
 * <p>Design intent: primitive arrays preserve deterministic layout and keep the
 * future native optimizer boundary straightforward.</p>
 */
public final class TacticalPolicyResult {
    public long version;
    public long createdAtNanos;
    public long strategicSubsetVersion;

    public int[] venueWeightBps;
    public int[] latencyPenaltyNanos;
    public int[] toxicityPenaltyBps;
    public int[] fillProbabilityBps;
    public int[] rejectPenaltyBps;
    public int[] queueSurvivalBps;
    public int[] slippagePenaltyBps;
    public int[] marketImpactPenaltyBps;

    public long[] minChildQty;
    public long[] maxChildQty;
    public int[] maxParticipationBps;

    public TacticalPolicyResult() {
    }
}
