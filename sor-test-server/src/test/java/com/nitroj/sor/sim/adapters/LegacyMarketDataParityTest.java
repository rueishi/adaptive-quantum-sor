package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-24 seeded market data parity behavior. */
final class LegacyMarketDataParityTest {
    @Test
    void fixedSeedProducesDeterministicBookAndTickCounter() {
        final SimConfig config = new SimConfig(2, 5, 3, 2);
        final SimulatedMarketDataSource first = new SimulatedMarketDataSource(config, 91L);
        final SimulatedMarketDataSource second = new SimulatedMarketDataSource(config, 91L);

        first.generateTick(SimRegime.VOLATILE, 0);
        second.generateTick(SimRegime.VOLATILE, 0);

        assertEquals(first.currentMidTicks(0), second.currentMidTicks(0));
        assertEquals(first.currentBidQty(1, 4), second.currentBidQty(1, 4));
        assertEquals(1, first.tick());
    }

    @Test
    void sanitizedUpdateKeepsBookValid() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource(new SimConfig(1, 1, 3, 1), 1L);
        source.applySanitized(0, 0, -10, -20, -5, -6);

        assertEquals(1L, source.currentBook().bidPriceTicks(0, 0));
        assertEquals(2L, source.currentBook().askPriceTicks(0, 0));
        assertEquals(0L, source.currentBidQty(0, 0));
    }
}
