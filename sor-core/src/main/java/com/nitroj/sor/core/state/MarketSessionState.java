package com.nitroj.sor.core.state;

/**
 * Responsibility: store whether each instrument is in an open market session.
 *
 * <p>Role in system: execution and regime logic can use this state to suppress
 * routing when an instrument is outside the simulated market session.</p>
 *
 * <p>Relationships: market session simulators update it; routing guards and
 * future regime detection read it.</p>
 *
 * <p>Lifecycle: allocated once and updated as simulated session time changes.</p>
 *
 * <p>Design intent: a boolean per instrument is enough for Phase 1 while keeping
 * calendars and exchange schedules out of scope.</p>
 */
public final class MarketSessionState {
    private final boolean[] openByInstrument;

    public MarketSessionState(final int instrumentCount) {
        if (instrumentCount <= 0) {
            throw new IllegalArgumentException("instrumentCount must be positive");
        }
        this.openByInstrument = new boolean[instrumentCount];
    }

    public void setOpen(final int instrumentId, final boolean open) {
        checkInstrument(instrumentId);
        openByInstrument[instrumentId] = open;
    }

    public boolean isOpen(final int instrumentId) {
        checkInstrument(instrumentId);
        return openByInstrument[instrumentId];
    }

    private void checkInstrument(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= openByInstrument.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
    }
}
