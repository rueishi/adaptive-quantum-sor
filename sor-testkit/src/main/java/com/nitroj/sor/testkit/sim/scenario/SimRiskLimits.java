package com.nitroj.sor.testkit.sim.scenario;

/** Simulator-local pre-trade risk limits. */
public final class SimRiskLimits {
    private final SimConfig config;
    private final long[] maxChildQty;
    private final long[] maxNotionalByCell;
    private final int[] maxParticipationBpsByCell;

    public SimRiskLimits(final SimConfig config) {
        this.config = config;
        this.maxChildQty = new long[config.instrumentCount()];
        this.maxNotionalByCell = new long[config.instrumentCount() * config.venueCount()];
        this.maxParticipationBpsByCell = new int[maxNotionalByCell.length];
    }

    public void setMaxChildQty(final int instrumentId, final long quantity) {
        config.checkInstrument(instrumentId);
        maxChildQty[instrumentId] = quantity;
    }

    public void setVenueLimits(final int instrumentId, final int venueId, final long notional, final int participationBps) {
        final int idx = config.cellIndex(instrumentId, venueId);
        maxNotionalByCell[idx] = notional;
        maxParticipationBpsByCell[idx] = participationBps;
    }

    public long maxChildQty(final int instrumentId) {
        config.checkInstrument(instrumentId);
        return maxChildQty[instrumentId];
    }

    public long maxNotional(final int instrumentId, final int venueId) {
        return maxNotionalByCell[config.cellIndex(instrumentId, venueId)];
    }

    public int maxParticipationBps(final int instrumentId, final int venueId) {
        return maxParticipationBpsByCell[config.cellIndex(instrumentId, venueId)];
    }
}
