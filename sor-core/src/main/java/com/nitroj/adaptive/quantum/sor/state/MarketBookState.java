package com.nitroj.adaptive.quantum.sor.state;

import com.nitroj.adaptive.quantum.sor.util.Indexing;

/**
 * Responsibility: store top-of-book market data by instrument and venue.
 *
 * <p>Role in system: Phase 1 routing can make deterministic decisions from this
 * compact L1 state before optional L2/L3 fidelity is added.</p>
 *
 * <p>Relationships: market simulators update this state; feature aggregation,
 * policy optimization, and execution read it. Index layout follows
 * {@link Indexing#idxIV(int, int)}.</p>
 *
 * <p>Lifecycle: allocated once for configured dimensions and updated in place
 * by simulated market data ticks.</p>
 *
 * <p>Design intent: validation prevents crossed books, negative prices, and
 * negative quantities from entering downstream routing logic.</p>
 */
public final class MarketBookState {
    private final Indexing indexing;
    private final long[] bidPriceTicks;
    private final long[] askPriceTicks;
    private final long[] bidQty;
    private final long[] askQty;
    private long sequence;

    public MarketBookState(final int instrumentCount, final int venueCount) {
        this.indexing = new Indexing(instrumentCount, venueCount, 1, 1);
        final int length = indexing.ivLength();
        this.bidPriceTicks = new long[length];
        this.askPriceTicks = new long[length];
        this.bidQty = new long[length];
        this.askQty = new long[length];
    }

    /**
     * Updates a top-of-book entry after validating market-data sanity.
     *
     * @param instrumentId dense instrument ID
     * @param venueId dense venue ID
     * @param bidPriceTicks positive bid price in ticks
     * @param askPriceTicks positive ask price in ticks, greater than bid
     * @param bidQty non-negative bid quantity
     * @param askQty non-negative ask quantity
     */
    public void updateTopOfBook(
            final int instrumentId,
            final int venueId,
            final long bidPriceTicks,
            final long askPriceTicks,
            final long bidQty,
            final long askQty
    ) {
        if (bidPriceTicks <= 0 || askPriceTicks <= 0) {
            throw new IllegalArgumentException("prices must be positive");
        }
        if (bidPriceTicks >= askPriceTicks) {
            throw new IllegalArgumentException("bid must be less than ask");
        }
        if (bidQty < 0 || askQty < 0) {
            throw new IllegalArgumentException("quantities must be non-negative");
        }
        final int idx = indexing.idxIV(instrumentId, venueId);
        this.bidPriceTicks[idx] = bidPriceTicks;
        this.askPriceTicks[idx] = askPriceTicks;
        this.bidQty[idx] = bidQty;
        this.askQty[idx] = askQty;
        sequence++;
    }

    public long bidPriceTicks(final int instrumentId, final int venueId) {
        return bidPriceTicks[indexing.idxIV(instrumentId, venueId)];
    }

    public long askPriceTicks(final int instrumentId, final int venueId) {
        return askPriceTicks[indexing.idxIV(instrumentId, venueId)];
    }

    public long bidQty(final int instrumentId, final int venueId) {
        return bidQty[indexing.idxIV(instrumentId, venueId)];
    }

    public long askQty(final int instrumentId, final int venueId) {
        return askQty[indexing.idxIV(instrumentId, venueId)];
    }

    public long sequence() {
        return sequence;
    }
}
