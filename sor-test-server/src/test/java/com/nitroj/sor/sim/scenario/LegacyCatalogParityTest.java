package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-23 instrument and venue catalog parity. */
final class LegacyCatalogParityTest {
    @Test
    void generatedCatalogsAreDenseEnabledAndFullySupported() {
        final SimConfig config = new SimConfig(3, 4, 3, 2);
        final SimInstrument[] instruments = new SimulatedInstrumentCatalog().generate(config);
        final SimVenue[] venues = new SimulatedVenueCatalog().generate(config);

        assertEquals(3, instruments.length);
        assertEquals("INSTRUMENT-2", instruments[2].symbol());
        assertTrue(instruments[2].enabled());
        assertEquals(4, venues.length);
        assertEquals("VENUE-3", venues[3].code());
        assertTrue(venues[3].enabled());
        assertTrue(venues[3].supports(2));
    }
}
