package com.nitroj.sor.testkit.sim.scenario;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-26 explicit scenario parent orders. */
final class SimulatedScenarioParentOrdersTest {
    @Test
    void explicitParentOrderOverridesGeneratedOrderForTick() {
        final SimulatedOrderInjector injector = new SimulatedOrderInjector(new SimConfig(1, 1, 3, 1), 1L);

        final List<SimParentOrder> orders = injector.ordersForTick(3, SimRegime.NORMAL, 9L,
                List.of(new SimScenarioParentOrder(3, 0, Side.SELL, 777, 0, 44)));

        assertEquals(1, orders.size());
        assertEquals(777, orders.get(0).quantity());
        assertEquals(44, orders.get(0).clientOrderRef());
    }
}
