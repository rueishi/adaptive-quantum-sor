package com.nitroj.adaptive.quantum.sor.state;

import com.nitroj.adaptive.quantum.sor.util.Indexing;

/**
 * Responsibility: provide a minimal L2 depth container for future simulators.
 *
 * <p>Role in system: Phase 1 routing uses top-of-book state, but the spec calls
 * for an optional L2 shell. This class stores price/quantity by
 * instrument/venue/level for later feature and optimizer work.</p>
 *
 * <p>Relationships: follows the same IV layout as {@link MarketBookState} and
 * adds a depth-level dimension local to this class.</p>
 *
 * <p>Lifecycle: allocated once with fixed depth and updated by simulated book
 * events.</p>
 *
 * <p>Design intent: the shell is intentionally simple and avoids matching-engine
 * semantics that are out of scope for P1-TC-004.</p>
 */
public final class L2DepthBook {
    private final Indexing indexing;
    private final int depthLevels;
    private final long[] bidPriceTicks;
    private final long[] bidQty;
    private final long[] askPriceTicks;
    private final long[] askQty;

    public L2DepthBook(final int instrumentCount, final int venueCount, final int depthLevels) {
        if (depthLevels <= 0) {
            throw new IllegalArgumentException("depthLevels must be positive");
        }
        this.indexing = new Indexing(instrumentCount, venueCount, 1, 1);
        this.depthLevels = depthLevels;
        final int length = indexing.ivLength() * depthLevels;
        this.bidPriceTicks = new long[length];
        this.bidQty = new long[length];
        this.askPriceTicks = new long[length];
        this.askQty = new long[length];
    }

    /**
     * Updates one L2 level after validating non-negative quantities and positive prices.
     */
    public void updateLevel(
            final int instrumentId,
            final int venueId,
            final int level,
            final long bidPriceTicks,
            final long bidQty,
            final long askPriceTicks,
            final long askQty
    ) {
        if (level < 0 || level >= depthLevels) {
            throw new IndexOutOfBoundsException("level out of range: " + level);
        }
        if (bidPriceTicks <= 0 || askPriceTicks <= 0 || bidQty < 0 || askQty < 0) {
            throw new IllegalArgumentException("prices must be positive and quantities non-negative");
        }
        if (bidPriceTicks >= askPriceTicks) {
            throw new IllegalArgumentException("bid must be less than ask");
        }
        final int idx = indexing.idxIV(instrumentId, venueId) * depthLevels + level;
        this.bidPriceTicks[idx] = bidPriceTicks;
        this.bidQty[idx] = bidQty;
        this.askPriceTicks[idx] = askPriceTicks;
        this.askQty[idx] = askQty;
    }

    public long bidPriceTicks(final int instrumentId, final int venueId, final int level) {
        return bidPriceTicks[index(instrumentId, venueId, level)];
    }

    public long bidQty(final int instrumentId, final int venueId, final int level) {
        return bidQty[index(instrumentId, venueId, level)];
    }

    public long askPriceTicks(final int instrumentId, final int venueId, final int level) {
        return askPriceTicks[index(instrumentId, venueId, level)];
    }

    public long askQty(final int instrumentId, final int venueId, final int level) {
        return askQty[index(instrumentId, venueId, level)];
    }

    private int index(final int instrumentId, final int venueId, final int level) {
        if (level < 0 || level >= depthLevels) {
            throw new IndexOutOfBoundsException("level out of range: " + level);
        }
        return indexing.idxIV(instrumentId, venueId) * depthLevels + level;
    }
}
