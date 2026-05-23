package com.nitroj.sor.sim.scenario;

/** Dense maker/taker fee snapshot using instrument/venue layout. */
public final class SimFeeScheduleSnapshot {
    private final SimConfig config;
    private final long[] makerBps;
    private final long[] takerBps;

    public SimFeeScheduleSnapshot(final SimConfig config) {
        this.config = config;
        this.makerBps = new long[config.instrumentCount() * config.venueCount()];
        this.takerBps = new long[makerBps.length];
    }

    public void setFees(final int instrumentId, final int venueId, final long maker, final long taker) {
        final int idx = config.cellIndex(instrumentId, venueId);
        makerBps[idx] = maker;
        takerBps[idx] = taker;
    }

    public long makerBps(final int instrumentId, final int venueId) {
        return makerBps[config.cellIndex(instrumentId, venueId)];
    }

    public long takerBps(final int instrumentId, final int venueId) {
        return takerBps[config.cellIndex(instrumentId, venueId)];
    }
}
