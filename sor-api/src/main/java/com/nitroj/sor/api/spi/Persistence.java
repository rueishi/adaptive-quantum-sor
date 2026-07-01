package com.nitroj.sor.api.spi;

import com.nitroj.sor.api.PolicyHandle;

import java.util.Iterator;

/**
 * Responsibility: durable lifecycle and policy snapshot SPI.
 *
 * <p>Role in system: integrators provide the write-ahead log implementation;
 * the engine calls this interface without knowing whether storage is in-memory,
 * Chronicle, or another adapter.</p>
 *
 * <p>Relationships: sequences {@link LifecycleEvent} and stores snapshots
 * identified by {@link PolicyHandle}.</p>
 *
 * <p>Lifecycle: configured before engine build and closed by the engine in
 * later implementation cards.</p>
 *
 * <p>Design intent: keep persistence explicit and replaceable without putting
 * databases or files on the routing path.</p>
 */
public interface Persistence {
    /**
     * Appends an event and assigns a strictly increasing sequence number.
     *
     * <p>Control-plane method, not hot-path for P8-04. Production adapters may
     * later optimize this warm path.</p>
     *
     * @param event lifecycle event to append
     * @return assigned sequence number
     */
    long appendLifecycleEvent(LifecycleEvent event);

    /**
     * Persists a write-once binary policy snapshot.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param handle policy identity handle
     * @param snapshotBytes serialized policy snapshot bytes
     */
    void persistPolicySnapshot(PolicyHandle handle, byte[] snapshotBytes);

    /**
     * Reads a previously stored policy snapshot by version.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param policyVersion policy version to read
     * @return serialized snapshot bytes, or an empty array when absent
     */
    byte[] readPolicySnapshot(long policyVersion);

    /**
     * Returns the latest persisted policy version, or zero if none exists.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return latest persisted policy version, or zero
     */
    long lastPersistedPolicyVersion();

    /**
     * Replays lifecycle events with sequence greater than `sinceSequence`.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param sinceSequence exclusive lower sequence bound
     * @return iterator over replayed lifecycle events
     */
    Iterator<LifecycleEvent> replay(long sinceSequence);
}
