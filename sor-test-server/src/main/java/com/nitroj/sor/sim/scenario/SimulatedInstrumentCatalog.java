package com.nitroj.sor.sim.scenario;

/**
 * Responsibility: generate and hold deterministic instrument metadata.
 *
 * <p>Role in system: replaces legacy instrument master-data simulation with a
 * simulator-local catalog that can be consumed without core metadata classes.</p>
 *
 * <p>Relationships: scenario setup and parity tests use generated
 * {@link SimInstrument} rows.</p>
 *
 * <p>Lifecycle: generated at scenario setup, then read as an immutable catalog
 * view for a run.</p>
 *
 * <p>Design intent: dense IDs are enabled and named consistently so metadata
 * parity stays deterministic.</p>
 */
public final class SimulatedInstrumentCatalog {
    /** Generates enabled dense instrument rows equivalent to legacy simulation. */
    public SimInstrument[] generate(final SimConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final SimInstrument[] rows = new SimInstrument[config.instrumentCount()];
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            rows[instrumentId] = new SimInstrument(instrumentId, "INSTRUMENT-" + instrumentId, true);
        }
        return rows;
    }
}
