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
     * Creates an empty reusable fill report.
     */
    public FillReport() {
    }

    /**
     * Updates every fill field for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param childOrderId engine-assigned child order identifier
     * @param parentOrderId engine-assigned parent order identifier
     * @param venueId venue reporting the fill
     * @param filledQuantity filled quantity
     * @param fillPrice fill price in fixed-point price units
     * @param filledEpochNanos wall-clock fill timestamp in epoch nanoseconds
     * @return this reusable report
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
     *
     * @return this reusable report
     */
    public FillReport clear() { return set(0, 0, 0, 0, 0, 0); }

    /**
     * Returns engine-assigned child order identifier.
     *
     * @return engine-assigned child order identifier
     */
    public long childOrderId() { return childOrderId; }
    /**
     * Returns engine-assigned parent order identifier.
     *
     * @return engine-assigned parent order identifier
     */
    public long parentOrderId() { return parentOrderId; }
    /**
     * Returns venue reporting the fill.
     *
     * @return venue reporting the fill
     */
    public int venueId() { return venueId; }
    /**
     * Returns filled quantity.
     *
     * @return filled quantity
     */
    public long filledQuantity() { return filledQuantity; }
    /**
     * Returns fill price in fixed-point price units.
     *
     * @return fill price in fixed-point price units
     */
    public long fillPrice() { return fillPrice; }
    /**
     * Returns wall-clock fill timestamp in epoch nanoseconds.
     *
     * @return wall-clock fill timestamp in epoch nanoseconds
     */
    public long filledEpochNanos() { return filledEpochNanos; }
}
