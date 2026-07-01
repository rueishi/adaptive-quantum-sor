package com.nitroj.sor.core.metadata;

/**
 * Responsibility: store static instrument metadata for dense Phase 1 IDs.
 *
 * <p>Role in system: policy linting, compilation, API views, and simulators use
 * instrument metadata to validate that an instrument ID is known and enabled.</p>
 *
 * <p>Relationships: {@link VenueMetadata} cross-references instrument support,
 * while risk and fee snapshots use the same dense instrument ID space.</p>
 *
 * <p>Lifecycle: generated from config during startup and treated as immutable
 * reference data for a run.</p>
 *
 * <p>Design intent: dense arrays keep lookup deterministic and avoid map keys in
 * routing-related validation paths.</p>
 */
public final class InstrumentMetadata {
    private final String[] symbols;
    private final boolean[] enabled;

    public InstrumentMetadata(final String[] symbols, final boolean[] enabled) {
        if (symbols == null || enabled == null || symbols.length == 0 || symbols.length != enabled.length) {
            throw new IllegalArgumentException("symbols and enabled must be non-empty arrays of equal length");
        }
        this.symbols = symbols.clone();
        this.enabled = enabled.clone();
        for (int i = 0; i < this.symbols.length; i++) {
            if (this.symbols[i] == null || this.symbols[i].isBlank()) {
                throw new IllegalArgumentException("instrument symbol must not be blank at index " + i);
            }
        }
    }

    /**
     * Creates deterministic simulated instruments.
     *
     * @param instrumentCount number of dense instruments
     * @return enabled metadata named INST0, INST1, ...
     */
    public static InstrumentMetadata simulated(final int instrumentCount) {
        if (instrumentCount <= 0) {
            throw new IllegalArgumentException("instrumentCount must be positive");
        }
        final String[] symbols = new String[instrumentCount];
        final boolean[] enabled = new boolean[instrumentCount];
        for (int i = 0; i < instrumentCount; i++) {
            symbols[i] = "INST" + i;
            enabled[i] = true;
        }
        return new InstrumentMetadata(symbols, enabled);
    }

    public boolean isEnabled(final int instrumentId) {
        checkInstrument(instrumentId);
        return enabled[instrumentId];
    }

    public String symbol(final int instrumentId) {
        checkInstrument(instrumentId);
        return symbols[instrumentId];
    }

    public int instrumentCount() {
        return symbols.length;
    }

    private void checkInstrument(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= symbols.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
    }
}
