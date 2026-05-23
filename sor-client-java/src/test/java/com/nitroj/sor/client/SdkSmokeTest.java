package com.nitroj.sor.client;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.sim.adapters.InMemoryPersistence;
import com.nitroj.sor.sim.adapters.ManualClock;
import com.nitroj.sor.sim.adapters.SimulatedMarketDataSource;
import com.nitroj.sor.sim.adapters.SimulatedRiskProvider;
import com.nitroj.sor.sim.adapters.SimulatedVenueAdapter;
import com.nitroj.sor.transport.aeron.AeronSorServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkSmokeTest {
    @Test
    void submitsOneOrderThroughAeronSdkFacade() {
        final ManualClock clock = new ManualClock(1_000);
        final SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(8, 100))
                .marketData(new SimulatedMarketDataSource())
                .venueAdapter(new SimulatedVenueAdapter(clock))
                .riskProvider(new SimulatedRiskProvider())
                .persistence(new InMemoryPersistence())
                .clock(clock)
                .build();
        engine.warmup(0);

        try (AeronSorServer server = new AeronSorServer("aeron:ipc", engine)) {
            server.start();
            try (AeronSorClient client = AeronSorClient.connect("aeron:ipc", SorClientConfig.defaults())) {
                assertTrue(client.isReady());
                final long id = client.submit(ParentOrderRequest.builder()
                        .instrumentId(0)
                        .side(Side.BUY)
                        .quantity(1)
                        .urgency(0)
                        .build());
                assertEquals(1, id);
            }
        }
    }
}
