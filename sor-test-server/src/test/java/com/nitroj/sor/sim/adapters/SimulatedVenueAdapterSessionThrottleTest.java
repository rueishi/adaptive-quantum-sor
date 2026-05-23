package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-25 session/throttle rejection behavior. */
final class SimulatedVenueAdapterSessionThrottleTest {
    @Test
    void haltedVenueRejectsWithoutFill() {
        final SimConfig config = new SimConfig(1, 5, 3, 1);
        final SimulatedMarketDataSource market = new SimulatedMarketDataSource(config, 1L);
        final SimulatedVenueAdapter venue = new SimulatedVenueAdapter(new ManualClock(), config, 1L);
        final SimVenueSessionSnapshot sessions = venue.outagePattern();

        final var outcomes = venue.process(new SimChildOrder(1, 1, 0, 4, Side.BUY, 100, 1, 0),
                market.currentBook(), sessions, venue.populateThrottle(), SimRegime.NORMAL);

        assertEquals("REJECT", outcomes.get(1).type());
    }
}
