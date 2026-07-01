package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.VenueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-23 market-session, venue-session, and throttle parity. */
final class LegacySessionThrottleParityTest {
    @Test
    void sessionsAndThrottlesFollowLegacyPatterns() {
        final SimConfig config = new SimConfig(2, 5, 3, 2);
        final SimulatedMarketDataSource market = new SimulatedMarketDataSource(config, 7L);
        final SimMarketSessionSnapshot marketSession = market.openAllSessions();
        assertTrue(marketSession.isOpen(0));
        market.applyMarketSession(marketSession, SimRegime.THIN_BOOK, 0);
        assertFalse(marketSession.isOpen(0));

        final SimulatedVenueAdapter venue = new SimulatedVenueAdapter(new ManualClock(), config, 9L);
        final SimVenueSessionSnapshot outage = venue.outagePattern();
        assertEquals(VenueStatus.HALTED, outage.status(4));
        final SimVenueThrottleSnapshot throttles = venue.populateThrottle();
        assertEquals(50, throttles.maxOrderRatePerSecond(0));
        assertEquals(70, throttles.maxOrderRatePerSecond(4));
        venue.populateThrottle(throttles, SimRegime.VOLATILE);
        assertEquals(25, throttles.maxOrderRatePerSecond(0));
    }
}
