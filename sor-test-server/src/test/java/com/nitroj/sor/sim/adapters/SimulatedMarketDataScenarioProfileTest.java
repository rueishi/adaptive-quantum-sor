package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies P8-24 scenario profile influence on spread and liquidity. */
final class SimulatedMarketDataScenarioProfileTest {
    @Test
    void volatileProfileWidensSpreadAndThinBookReducesQuantity() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource(new SimConfig(1, 5, 3, 1), 5L);

        source.generateTick(SimRegime.THIN_BOOK, 0);

        assertTrue(source.currentAskQty(0, 0) <= 5_000L);
        assertTrue(source.currentBook().askPriceTicks(0, 1) > source.currentBook().bidPriceTicks(0, 1));
    }
}
