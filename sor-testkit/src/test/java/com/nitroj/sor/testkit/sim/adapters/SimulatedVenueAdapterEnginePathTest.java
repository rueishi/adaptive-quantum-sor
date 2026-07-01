package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: verifies the simulator venue adapter can consume child
 * orders emitted by the real engine path.
 *
 * <p>Role in system: proves the test-server simulator no longer needs direct
 * child-order construction to drive a fill callback.</p>
 *
 * <p>Relationships: composes {@link SimulatedMarketDataSource},
 * {@link SimulatedVenueAdapter}, {@link SimulatedRiskProvider}, and
 * {@link InMemoryPersistence} through public `SorEngineBuilder` wiring.</p>
 *
 * <p>Lifecycle: builds an embedded engine, submits one parent order, polls the
 * simulator venue ring, then closes the engine.</p>
 *
 * <p>Design intent: keep simulator polling deterministic while exercising the
 * production venue SPI handoff.</p>
 */
class SimulatedVenueAdapterEnginePathTest {
    /**
     * Confirms simulator polling consumes the engine-emitted child order and
     * delivers a fill callback that updates public order status.
     */
    @Test
    void simulatorPollConsumesEngineChildOrderAndFillsParent() {
        final ManualClock clock = new ManualClock(1_000);
        final SimulatedVenueAdapter venue = new SimulatedVenueAdapter(clock);
        final SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1))
                .marketData(new SimulatedMarketDataSource())
                .venueAdapter(venue)
                .riskProvider(new SimulatedRiskProvider())
                .persistence(new InMemoryPersistence())
                .clock(clock)
                .build();
        engine.warmup(0);

        final long parentOrderId = engine.submitParentOrder(ParentOrderRequest.builder()
                .instrumentId(0)
                .side(Side.BUY)
                .quantity(25)
                .urgency(0)
                .build());

        assertEquals(1, venue.pollVenue(0, 1));
        assertEquals(OrderStatusCode.FILLED, engine.getOrderStatus(parentOrderId).orElseThrow().status());

        engine.close();
    }
}
