package com.nitroj.sor.core;

import com.nitroj.sor.api.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies warmup timeout failure keeps readiness false. */
class WarmupTimeoutTest {
    @Test
    void slowClockCausesWarmupTimeout() {
        final EngineTestSupport.ManualClock clock = new EngineTestSupport.ManualClock() {
            @Override public long nanoTime() {
                advance(10_000_000L);
                return super.nanoTime();
            }
        };
        final var engine = com.nitroj.sor.api.SorEngineBuilder.create()
                .config(new SorConfig(1024, 1))
                .marketData(new EngineTestSupport.FakeMarketData())
                .venueAdapter(new EngineTestSupport.FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new EngineTestSupport.FakePersistence())
                .clock(clock)
                .build();
        assertThrows(WarmupTimeoutException.class, () -> engine.warmup(10));
        assertFalse(engine.isReady());
    }
}
