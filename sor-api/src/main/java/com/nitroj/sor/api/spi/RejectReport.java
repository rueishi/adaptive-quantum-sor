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
     * Updates every reject field for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
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
     */
    public RejectReport clear() { return set(0, 0, 0, 0, 0); }

    public long childOrderId() { return childOrderId; }
    public long parentOrderId() { return parentOrderId; }
    public int venueId() { return venueId; }
    public int reasonCode() { return reasonCode; }
    public long rejectedEpochNanos() { return rejectedEpochNanos; }
}
