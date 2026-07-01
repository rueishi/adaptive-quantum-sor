package com.nitroj.sor.testkit.sim.scenario;

/** Public simulator snapshot of open/closed state per instrument. */
public final class SimMarketSessionSnapshot {
    private final boolean[] openByInstrument;

    public SimMarketSessionSnapshot(final int instrumentCount) {
        this.openByInstrument = new boolean[instrumentCount];
    }

    public void setOpen(final int instrumentId, final boolean open) {
        openByInstrument[instrumentId] = open;
    }

    public boolean isOpen(final int instrumentId) {
        return openByInstrument[instrumentId];
    }
}
