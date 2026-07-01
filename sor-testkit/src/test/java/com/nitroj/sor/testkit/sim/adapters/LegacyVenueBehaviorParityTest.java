package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-25 venue ACK/fill/reject parity behavior. */
final class LegacyVenueBehaviorParityTest {
    @Test
    void executableVenueProducesAckAndTerminalOutcome() {
        final SimConfig config = new SimConfig(1, 1, 3, 1);
        final ManualClock clock = new ManualClock();
        final SimulatedMarketDataSource market = new SimulatedMarketDataSource(config, 1L);
        market.applySanitized(0, 0, 99, 101, 10_000, 10_000);
        final SimulatedVenueAdapter venue = new SimulatedVenueAdapter(clock, config, 1L);
        final List<SimVenueOutcome> outcomes = venue.process(
                new SimChildOrder(1, 1, 0, 0, Side.BUY, 1_000, 101, 0),
                market.currentBook(), venue.openAllSessions(), venue.populateThrottle(), SimRegime.NORMAL);

        assertEquals("ACK", outcomes.get(0).type());
        assertTrue(outcomes.get(1).type().endsWith("FILL") || "REJECT".equals(outcomes.get(1).type()));
    }
}
