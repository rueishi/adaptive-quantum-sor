package com.nitroj.sor.testserver;

import com.nitroj.sor.core.policy.SorPolicy;

/**
 * Responsibility: expose active policy identity to API clients.
 *
 * <p>Role in system: GET /policy/current returns this compact control-plane
 * view. Notebooks use it to confirm which policy version/hash was active for a
 * demo order or scenario run.</p>
 *
 * <p>Relationships: built from {@link SorPolicy} by
 * {@link NotebookScenarioHttpServer}. It intentionally copies only stable
 * identity fields instead of serializing the full route book or policy matrix.</p>
 *
 * <p>Lifecycle: created per API response.</p>
 *
 * <p>Design intent: avoid leaking large internal policy arrays through the
 * notebook/demo API while still providing enough provenance to correlate
 * routing decisions, lifecycle events, and scenario summaries.</p>
 */
public final class PolicySnapshotView {
    public final long policyVersion;
    public final long policyHash64;

    public PolicySnapshotView(final SorPolicy policy) {
        this.policyVersion = policy == null ? 0L : policy.policyVersion;
        this.policyHash64 = policy == null ? 0L : policy.policyHash64;
    }

    public String toJson() {
        return "{\"policyVersion\":" + policyVersion + ",\"policyHash64\":" + policyHash64 + "}";
    }
}
