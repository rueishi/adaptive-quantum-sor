package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable lifecycle event persisted by the framework.
 *
 * <p>Role in system: the engine appends these through {@link Persistence};
 * persistence assigns the sequence number.</p>
 *
 * <p>Relationships: used for replay and crash recovery in later cards.</p>
 *
 * <p>Lifecycle: created or reused by the engine, sequenced by persistence, and
 * replayed to recovery consumers.</p>
 *
 * <p>Design intent: provide a primitive event envelope without binding the API
 * to a concrete WAL implementation.</p>
 */
public final class LifecycleEvent {
    private long sequenceNumber;
    private long eventType;
    private long subjectId;
    private long epochNanos;

    /**
     * Creates an empty reusable lifecycle event.
     */
    public LifecycleEvent() {
    }

    /**
     * Updates every lifecycle field except sequence.
     *
     * <p>Control-plane method, not hot-path. Persistence paths may allocate
     * outside routing.</p>
     *
     * @param eventType stable lifecycle event type identifier
     * @param subjectId subject identifier for the lifecycle event
     * @param epochNanos wall-clock event timestamp in epoch nanoseconds
     * @return this reusable event
     */
    public LifecycleEvent set(final long eventType, final long subjectId, final long epochNanos) {
        this.eventType = eventType;
        this.subjectId = subjectId;
        this.epochNanos = epochNanos;
        return this;
    }

    /**
     * Assigns the monotonic persistence sequence number.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param sequenceNumber monotonic persistence sequence number
     * @return this reusable event
     */
    public LifecycleEvent sequenceNumber(final long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
        return this;
    }

    /**
     * Clears all fields to zero.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return this reusable event
     */
    public LifecycleEvent clear() {
        this.sequenceNumber = 0;
        return set(0, 0, 0);
    }

    /**
     * Returns monotonic persistence sequence number.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return monotonic persistence sequence number
     */
    public long sequenceNumber() { return sequenceNumber; }
    /**
     * Returns stable lifecycle event type identifier.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return stable lifecycle event type identifier
     */
    public long eventType() { return eventType; }
    /**
     * Returns subject identifier for the lifecycle event.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return subject identifier for the lifecycle event
     */
    public long subjectId() { return subjectId; }
    /**
     * Returns wall-clock event timestamp in epoch nanoseconds.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return wall-clock event timestamp in epoch nanoseconds
     */
    public long epochNanos() { return epochNanos; }
}
