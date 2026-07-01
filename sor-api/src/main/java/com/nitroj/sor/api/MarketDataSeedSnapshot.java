package com.nitroj.sor.api;

import java.util.HashSet;

/**
 * Responsibility: immutable startup market-data snapshot.
 *
 * <p>Role in system: provides venue-aware top-of-book cells and as-of metadata
 * for control-plane market hydration.</p>
 *
 * <p>Relationships: referenced by {@link SorStartupHydrationRequest} and
 * supplied by {@link SorStartupStateSource}.</p>
 *
 * <p>Lifecycle: created once per startup/recovery attempt and defensively
 * copied by the API record.</p>
 *
 * <p>Design intent: prevent duplicate market cells and mutable caller arrays
 * from leaking into engine startup logic.</p>
 *
 * @param snapshotId source snapshot identifier
 * @param asOfSequence source sequence watermark
 * @param asOfEpochNanos source as-of timestamp
 * @param cells populated venue-aware top-of-book cells
 */
public record MarketDataSeedSnapshot(
        String snapshotId,
        long asOfSequence,
        long asOfEpochNanos,
        MarketDataSeedCell[] cells
) {
    /**
     * Validates metadata, rejects duplicate instrument/venue cells, and copies
     * the caller-provided array.
     */
    public MarketDataSeedSnapshot {
        if (snapshotId == null || snapshotId.isBlank() || asOfSequence < 0
                || asOfEpochNanos < 0 || cells == null) {
            throw new IllegalArgumentException("market data seed snapshot inputs must be valid");
        }
        cells = cells.clone();
        final HashSet<Long> seen = new HashSet<>();
        for (MarketDataSeedCell cell : cells) {
            if (cell == null) {
                throw new IllegalArgumentException("market data seed cells must not contain null");
            }
            final long key = (((long) cell.instrumentId()) << 32) ^ (cell.venueId() & 0xffff_ffffL);
            if (!seen.add(key)) {
                throw new IllegalArgumentException("market data seed snapshot contains duplicate instrument/venue cell");
            }
        }
    }

    /**
     * Returns a defensive copy of the market-data seed cells.
     *
     * @return market-data seed cells
     */
    @Override
    public MarketDataSeedCell[] cells() {
        return cells.clone();
    }

    /**
     * Returns a stable checksum over source metadata and cells.
     *
     * @return deterministic checksum
     */
    public long checksum() {
        long checksum = snapshotId.hashCode() * 31L + asOfSequence;
        checksum = checksum * 31L + asOfEpochNanos;
        for (MarketDataSeedCell cell : cells) {
            checksum = checksum * 31L + cell.instrumentId();
            checksum = checksum * 31L + cell.venueId();
            checksum = checksum * 31L + cell.bidPrice();
            checksum = checksum * 31L + cell.askPrice();
            checksum = checksum * 31L + cell.bidQuantity();
            checksum = checksum * 31L + cell.askQuantity();
            checksum = checksum * 31L + cell.epochNanos();
        }
        return checksum;
    }
}
