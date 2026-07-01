package com.nitroj.sor.testkit.sim.scenario;

/** Simulator-local instrument metadata row. */
public record SimInstrument(int instrumentId, String symbol, boolean enabled) {
    public SimInstrument {
        if (instrumentId < 0) {
            throw new IllegalArgumentException("instrumentId must be non-negative");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }
    }
}
