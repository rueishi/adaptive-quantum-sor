package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-26 parent-order generator parity. */
final class LegacyParentOrderInjectorParityTest {
    @Test
    void fixedSeedGeneratesSameNormalOrder() {
        final SimConfig config = new SimConfig(2, 5, 3, 2);
        final SimParentOrder first = new SimulatedOrderInjector(config, 11L).next(5L);
        final SimParentOrder second = new SimulatedOrderInjector(config, 11L).next(5L);

        assertEquals(first, second);
        assertTrue(first.quantity() >= 100 && first.quantity() <= 10_000);
    }
}
