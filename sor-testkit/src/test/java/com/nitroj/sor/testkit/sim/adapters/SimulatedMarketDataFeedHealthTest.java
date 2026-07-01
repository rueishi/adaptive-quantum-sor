package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-24 feed-health tick reporting. */
final class SimulatedMarketDataFeedHealthTest {
    @Test
    void feedHealthTracksGeneratedTicks() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource(new SimConfig(1, 5, 3, 1), 3L);

        source.generateTick();

        assertEquals(1L, source.feedHealth().lastTick(0));
    }
}
