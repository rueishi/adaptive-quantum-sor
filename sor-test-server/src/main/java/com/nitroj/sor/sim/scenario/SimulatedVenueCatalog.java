package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.scenario.venues.SimVenue;

/**
 * Responsibility: generate and hold deterministic venue metadata.
 *
 * <p>Role in system: supplies enabled dense venues and instrument-support
 * matrices to simulator scenarios without using core metadata classes.</p>
 *
 * <p>Relationships: generated {@link SimVenue} rows align with
 * {@link SimulatedInstrumentCatalog} dimensions.</p>
 *
 * <p>Lifecycle: generated at scenario setup, then read as an immutable catalog
 * view for a run.</p>
 *
 * <p>Design intent: preserve the legacy assumption that all generated venues
 * are enabled and support every generated instrument.</p>
 */
public final class SimulatedVenueCatalog {
    /** Generates enabled dense venue rows with full instrument support. */
    public SimVenue[] generate(final SimConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final SimVenue[] rows = new SimVenue[config.venueCount()];
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final boolean[] supported = new boolean[config.instrumentCount()];
            java.util.Arrays.fill(supported, true);
            rows[venueId] = new SimVenue(venueId, "VENUE-" + venueId, true, supported);
        }
        return rows;
    }
}
