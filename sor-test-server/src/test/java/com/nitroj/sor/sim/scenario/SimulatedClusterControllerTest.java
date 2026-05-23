package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedClusterControllerTest {
    @Test
    void coordinatesMarketTicksAndVenuePolling() {
        final ManualClock clock = new ManualClock(5);
        final SimulatedMarketDataSource marketData = new SimulatedMarketDataSource();
        final SimulatedVenueAdapter venueAdapter = new SimulatedVenueAdapter(clock);
        final SimulatedClusterController controller = new SimulatedClusterController(clock, marketData, venueAdapter);
        final long[] quoteTime = {0};
        marketData.subscribe(1, quote -> quoteTime[0] = quote.epochNanos());

        controller.publishTick(1, 10, 11);

        assertEquals(5, quoteTime[0]);
        assertEquals(0, controller.pollVenue(1, 10));
    }
}
