package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable top-of-book quote carrier.
 *
 * <p>Role in system: market data sources populate one instance repeatedly and
 * pass it to the engine listener.</p>
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
    private long bidPrice;
    private long askPrice;
    private long bidQuantity;
    private long askQuantity;
    private long epochNanos;

    /**
     * Updates every quote field and returns this carrier for reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public Quote set(final int instrumentId, final long bidPrice, final long askPrice,
                     final long bidQuantity, final long askQuantity, final long epochNanos) {
        this.instrumentId = instrumentId;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.bidQuantity = bidQuantity;
        this.askQuantity = askQuantity;
        this.epochNanos = epochNanos;
        return this;
    }

    /**
     * Clears all fields to zero for deterministic reuse.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public Quote clear() {
        return set(0, 0, 0, 0, 0, 0);
    }

    public int instrumentId() { return instrumentId; }
    public long bidPrice() { return bidPrice; }
    public long askPrice() { return askPrice; }
    public long bidQuantity() { return bidQuantity; }
    public long askQuantity() { return askQuantity; }
    public long epochNanos() { return epochNanos; }
}
