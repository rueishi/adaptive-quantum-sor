package com.nitroj.sor.testkit.sim.scenario;

/** Public dense top-of-book snapshot used by venue and core scenario orchestration. */
public final class SimMarketBookSnapshot {
    private final SimConfig config;
    private final long[] bid;
    private final long[] ask;
    private final long[] bidQty;
    private final long[] askQty;

    public SimMarketBookSnapshot(final SimConfig config) {
        this.config = config;
        final int cells = config.instrumentCount() * config.venueCount();
        this.bid = new long[cells];
        this.ask = new long[cells];
        this.bidQty = new long[cells];
        this.askQty = new long[cells];
    }

    public void updateTopOfBook(
            final int instrumentId,
            final int venueId,
            final long bidPrice,
            final long askPrice,
            final long bidQuantity,
            final long askQuantity
    ) {
        final int idx = config.cellIndex(instrumentId, venueId);
        bid[idx] = Math.max(1L, bidPrice);
        ask[idx] = Math.max(bid[idx] + 1L, askPrice);
        bidQty[idx] = Math.max(0L, bidQuantity);
        askQty[idx] = Math.max(0L, askQuantity);
    }

    public long bidPriceTicks(final int instrumentId, final int venueId) {
        return bid[config.cellIndex(instrumentId, venueId)];
    }

    public long askPriceTicks(final int instrumentId, final int venueId) {
        return ask[config.cellIndex(instrumentId, venueId)];
    }

    public long bidQty(final int instrumentId, final int venueId) {
        return bidQty[config.cellIndex(instrumentId, venueId)];
    }

    public long askQty(final int instrumentId, final int venueId) {
        return askQty[config.cellIndex(instrumentId, venueId)];
    }
}
