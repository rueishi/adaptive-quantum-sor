package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-25 zero-liquidity rejection behavior. */
final class SimulatedVenueAdapterLiquidityOutcomeTest {
    @Test
    void zeroVisibleLiquidityRejects() {
        final SimConfig config = new SimConfig(1, 1, 3, 1);
        final SimulatedMarketDataSource market = new SimulatedMarketDataSource(config, 1L);
        market.applySanitized(0, 0, 10, 11, 0, 0);
        final SimulatedVenueAdapter venue = new SimulatedVenueAdapter(new ManualClock(), config, 1L);

        final var outcomes = venue.process(new SimChildOrder(1, 1, 0, 0, Side.BUY, 100, 11, 0),
                market.currentBook(), venue.openAllSessions(), venue.populateThrottle(), SimRegime.NORMAL);

        assertEquals("REJECT", outcomes.get(1).type());
    }
}
