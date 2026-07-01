package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.testkit.sim.adapters.InMemoryPersistence;
import com.nitroj.sor.testkit.sim.adapters.ManualClock;
import com.nitroj.sor.testkit.sim.adapters.SimulatedMarketDataSource;
import com.nitroj.sor.testkit.sim.adapters.SimulatedRiskProvider;
import com.nitroj.sor.testkit.sim.adapters.SimulatedVenueAdapter;

/**
 * Provides shared fixtures for Aeron transport tests.
 *
 * <p>Use it from transport tests to keep fake engines and channel setup consistent.</p>
 */
final class AeronTestSupport {
    private AeronTestSupport() {}

    static SorEngine engine(final int capacity) {
        final ManualClock clock = new ManualClock(1_000);
        final SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(capacity, 100))
                .marketData(new SimulatedMarketDataSource())
                .venueAdapter(new SimulatedVenueAdapter(clock))
                .riskProvider(new SimulatedRiskProvider())
                .persistence(new InMemoryPersistence())
                .clock(clock)
                .build();
        engine.warmup(0);
        return engine;
    }

    static ParentOrderRequest request() {
        return ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(1).urgency(0).build();
    }
}
