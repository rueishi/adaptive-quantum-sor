package com.nitroj.sor.core.state;

import com.nitroj.sor.core.util.Indexing;

import java.util.Arrays;

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
    private long lastUpdateEpochNanos;

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
        updateTopOfBook(instrumentId, venueId, bidPriceTicks, askPriceTicks, bidQty, askQty, 0L);
    }

    /**
     * Updates a top-of-book entry after validating market-data sanity and
     * records the source update timestamp.
     *
     * <p>Hot-path method. Must not allocate.</p>
     */
    public void updateTopOfBook(
            final int instrumentId,
            final int venueId,
            final long bidPriceTicks,
            final long askPriceTicks,
            final long bidQty,
            final long askQty,
            final long epochNanos
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
        lastUpdateEpochNanos = Math.max(lastUpdateEpochNanos, epochNanos);
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

    /**
     * Clears every top-of-book cell and resets the sequence.
     *
     * <p>Control-plane method, not hot-path. Used by explicit reset/purge
     * operations before deterministic scenario repopulation.</p>
     */
    public void clear() {
        Arrays.fill(bidPriceTicks, 0L);
        Arrays.fill(askPriceTicks, 0L);
        Arrays.fill(bidQty, 0L);
        Arrays.fill(askQty, 0L);
        sequence = 0L;
        lastUpdateEpochNanos = 0L;
    }

    public long lastUpdateEpochNanos() {
        return lastUpdateEpochNanos;
    }

    public int populatedCellCount() {
        int count = 0;
        for (int i = 0; i < bidPriceTicks.length; i++) {
            if (bidPriceTicks[i] != 0 || askPriceTicks[i] != 0 || bidQty[i] != 0 || askQty[i] != 0) {
                count++;
            }
        }
        return count;
    }

    public long checksum() {
        long checksum = sequence * 31L + lastUpdateEpochNanos;
        for (int i = 0; i < bidPriceTicks.length; i++) {
            checksum = checksum * 31L + bidPriceTicks[i];
            checksum = checksum * 31L + askPriceTicks[i];
            checksum = checksum * 31L + bidQty[i];
            checksum = checksum * 31L + askQty[i];
        }
        return checksum;
    }
}
