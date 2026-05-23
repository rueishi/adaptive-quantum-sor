package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable venue fill report.
 *
 * <p>Role in system: venue adapters populate this carrier and deliver it
 * through {@link VenueAdapter.VenueAdapterCallback#deliverFill(FillReport)}.</p>
 *
 * <p>Relationships: mirrors fill events without exposing venue implementation
 * objects.</p>
 *
 * <p>Lifecycle: owned by the adapter thread and reused after callback return.</p>
 *
 * <p>Design intent: keep inbound venue reports primitive and allocation-free.</p>
 */
public final class FillReport {
    private long childOrderId;
    private long parentOrderId;
    private int venueId;
    private long filledQuantity;
    private long fillPrice;
    private long filledEpochNanos;

    /**
     * Updates every fill field for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public FillReport set(final long childOrderId, final long parentOrderId, final int venueId,
                          final long filledQuantity, final long fillPrice, final long filledEpochNanos) {
        this.childOrderId = childOrderId;
        this.parentOrderId = parentOrderId;
        this.venueId = venueId;
        this.filledQuantity = filledQuantity;
        this.fillPrice = fillPrice;
        this.filledEpochNanos = filledEpochNanos;
        return this;
    }

    /**
     * Clears all fields to zero.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public FillReport clear() { return set(0, 0, 0, 0, 0, 0); }

    public long childOrderId() { return childOrderId; }
    public long parentOrderId() { return parentOrderId; }
    public int venueId() { return venueId; }
    public long filledQuantity() { return filledQuantity; }
    public long fillPrice() { return fillPrice; }
    public long filledEpochNanos() { return filledEpochNanos; }
}
