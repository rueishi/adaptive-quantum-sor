package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable venue reject report.
 *
 * <p>Role in system: venue adapters populate this carrier and deliver it
 * through {@link VenueAdapter.VenueAdapterCallback#deliverReject(RejectReport)}.</p>
 *
 * <p>Relationships: mirrors reject events without exposing venue implementation
 * objects.</p>
 *
 * <p>Lifecycle: owned by the adapter thread and reused after callback return.</p>
 *
 * <p>Design intent: keep rejection reporting primitive and allocation-free.</p>
 */
public final class RejectReport {
    private long childOrderId;
    private long parentOrderId;
    private int venueId;
    private int reasonCode;
    private long rejectedEpochNanos;

    /**
     * Creates an empty reusable rejectReport.
     */
    public RejectReport() {
    }

    /**
     * Updates every reject field for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param childOrderId engine-assigned child order identifier
     * @param parentOrderId engine-assigned parent order identifier
     * @param venueId venue reporting the rejection
     * @param reasonCode rejection reason code
     * @param rejectedEpochNanos wall-clock rejection timestamp in epoch nanoseconds
     * @return this reusable report
     */
    public RejectReport set(final long childOrderId, final long parentOrderId, final int venueId,
                            final int reasonCode, final long rejectedEpochNanos) {
        this.childOrderId = childOrderId;
        this.parentOrderId = parentOrderId;
        this.venueId = venueId;
        this.reasonCode = reasonCode;
        this.rejectedEpochNanos = rejectedEpochNanos;
        return this;
    }

    /**
     * Clears all fields to zero.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return this reusable report
     */
    public RejectReport clear() { return set(0, 0, 0, 0, 0); }

    /**
     * Returns the engine-assigned child order identifier.
     *
     * @return engine-assigned child order identifier
     */
    public long childOrderId() { return childOrderId; }
    /**
     * Returns the engine-assigned parent order identifier.
     *
     * @return engine-assigned parent order identifier
     */
    public long parentOrderId() { return parentOrderId; }
    /**
     * Returns the venue reporting the rejection.
     *
     * @return venue reporting the rejection
     */
    public int venueId() { return venueId; }
    /**
     * Returns the rejection reason code.
     *
     * @return rejection reason code
     */
    public int reasonCode() { return reasonCode; }
    /**
     * Returns the wall-clock rejection timestamp in epoch nanoseconds.
     *
     * @return wall-clock rejection timestamp in epoch nanoseconds
     */
    public long rejectedEpochNanos() { return rejectedEpochNanos; }
}
