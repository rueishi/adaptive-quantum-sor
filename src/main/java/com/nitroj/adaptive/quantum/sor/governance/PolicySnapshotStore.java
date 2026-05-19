package com.nitroj.adaptive.quantum.sor.governance;

import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

/**
 * Responsibility: define the policy snapshot persistence boundary.
 *
 * <p>Role in system: publication and rollback workflows use this interface to
 * store and retrieve policy snapshots without coupling the compiler to a
 * concrete persistence format.</p>
 *
 * <p>Relationships: implementations will be called by policy publisher or
 * governance services and return {@link SorPolicy} instances to replay or API
 * layers.</p>
 *
 * <p>Lifecycle: Phase 1 defines the contract only; binary persistence is
 * explicitly outside this task card.</p>
 *
 * <p>Design intent: keep persistence replaceable while preserving the minimum
 * operations required for publish and rollback stories.</p>
 */
public interface PolicySnapshotStore {
    /**
     * Stores a policy snapshot for later audit, replay, or rollback.
     */
    void store(SorPolicy policy);

    /**
     * Loads a policy snapshot by version.
     *
     * @return matching policy, or {@code null} when the snapshot is absent
     */
    SorPolicy load(long policyVersion);
}
