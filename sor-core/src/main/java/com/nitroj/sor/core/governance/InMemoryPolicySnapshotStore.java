package com.nitroj.sor.core.governance;

import com.nitroj.sor.core.policy.SorPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsibility: store policy snapshots in memory for Phase 1.
 *
 * <p>Role in system: publication and rollback workflows need a snapshot store
 * before binary persistence is introduced.</p>
 *
 * <p>Relationships: implements {@link PolicySnapshotStore} and is used by the
 * policy publisher.</p>
 *
 * <p>Lifecycle: created during startup or tests and retained for the process
 * lifetime.</p>
 *
 * <p>Design intent: a map-backed implementation is sufficient for deterministic
 * Adaptive Quantum SOR rollback tests while binary persistence remains out of scope.</p>
 */
public final class InMemoryPolicySnapshotStore implements PolicySnapshotStore {
    private final Map<Long, SorPolicy> snapshots = new HashMap<>();

    @Override
    public void store(final SorPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        snapshots.put(policy.policyVersion, policy);
    }

    @Override
    public SorPolicy load(final long policyVersion) {
        return snapshots.get(policyVersion);
    }

    public int size() {
        return snapshots.size();
    }
}
