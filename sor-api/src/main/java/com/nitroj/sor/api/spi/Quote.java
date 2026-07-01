package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable top-of-book quote carrier.
 *
 * <p>Role in system: market data sources populate one instance repeatedly and
 * pass it to the engine listener. Quotes are keyed by instrument and venue so
 * the engine can maintain its own per-venue routing book.</p>
 *
 * <p>Relationships: delivered through {@link MarketDataListener#onQuote}.</p>
 *
 * <p>Lifecycle: owned and reused by the source; listeners must not retain it
 * beyond callback return.</p>
 *
 * <p>Design intent: avoid hot-path allocation by using primitive mutable
 * fields.</p>
 */
public final class Quote {
    private int instrumentId;
    private int venueId;
    private long bidPrice;
    private long askPrice;
    private long bidQuantity;
    private long askQuantity;
    private long epochNanos;

    /**
     * Creates an empty reusable quote.
     */
    public Quote() {
    }

    /**
     * Updates every quote field and returns this carrier for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param instrumentId instrument identifier
     * @param venueId venue identifier
     * @param bidPrice best bid price in fixed-point price units
     * @param askPrice best ask price in fixed-point price units
     * @param bidQuantity displayed bid quantity
     * @param askQuantity displayed ask quantity
     * @param epochNanos quote timestamp in epoch nanoseconds
     * @return this reusable quote
     */
    public Quote set(final int instrumentId, final int venueId, final long bidPrice, final long askPrice,
                     final long bidQuantity, final long askQuantity, final long epochNanos) {
        this.instrumentId = instrumentId;
        this.venueId = venueId;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.bidQuantity = bidQuantity;
        this.askQuantity = askQuantity;
        this.epochNanos = epochNanos;
        return this;
    }

    /**
     * Updates quote fields for legacy instrument-only publishers and assigns
     * venue zero.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param instrumentId instrument identifier
     * @param bidPrice best bid price in fixed-point price units
     * @param askPrice best ask price in fixed-point price units
     * @param bidQuantity displayed bid quantity
     * @param askQuantity displayed ask quantity
     * @param epochNanos quote timestamp in epoch nanoseconds
     * @return this reusable quote
     */
    public Quote set(final int instrumentId, final long bidPrice, final long askPrice,
                     final long bidQuantity, final long askQuantity, final long epochNanos) {
        return set(instrumentId, 0, bidPrice, askPrice, bidQuantity, askQuantity, epochNanos);
    }

    /**
     * Clears all fields to zero for deterministic reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return this reusable quote
     */
    public Quote clear() {
        return set(0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Returns the instrument identifier.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return instrument identifier
     */
    public int instrumentId() { return instrumentId; }
    /**
     * Returns the venue identifier.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return venue identifier
     */
    public int venueId() { return venueId; }
    /**
     * Returns the best bid price in fixed-point price units.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return best bid price
     */
    public long bidPrice() { return bidPrice; }
    /**
     * Returns the best ask price in fixed-point price units.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return best ask price
     */
    public long askPrice() { return askPrice; }
    /**
     * Returns the displayed bid quantity.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return displayed bid quantity
     */
    public long bidQuantity() { return bidQuantity; }
    /**
     * Returns the displayed ask quantity.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return displayed ask quantity
     */
    public long askQuantity() { return askQuantity; }
    /**
     * Returns the quote timestamp in epoch nanoseconds.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @return quote timestamp in epoch nanoseconds
     */
    public long epochNanos() { return epochNanos; }
}
