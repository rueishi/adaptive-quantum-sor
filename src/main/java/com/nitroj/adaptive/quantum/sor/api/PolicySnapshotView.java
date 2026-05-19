package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

/**
 * Responsibility: expose active policy identity to API clients.
 *
 * <p>Role in system: GET /policy/current returns this compact control-plane
 * view.</p>
 *
 * <p>Relationships: built from {@link SorPolicy} by {@link SorHttpApiServer}.</p>
 *
 * <p>Lifecycle: created per API response.</p>
 *
 * <p>Design intent: avoid serializing full route arrays in the MVP API.</p>
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
