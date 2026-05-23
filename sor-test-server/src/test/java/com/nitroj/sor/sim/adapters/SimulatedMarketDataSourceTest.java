package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedMarketDataSourceTest {
    @Test
    void subscribedListenerReceivesDeterministicQuoteFields() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final long[] captured = new long[6];

        source.subscribe(7, quote -> copy(quote, captured));
        source.publish(7, 101, 103, 10, 11, 12345);

        assertArrayEquals(new long[] {7, 101, 103, 10, 11, 12345}, captured);
    }

    @Test
    void unsubscribeStopsDelivery() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final int[] count = {0};
        final com.nitroj.sor.api.spi.MarketDataListener listener = quote -> count[0]++;

        source.subscribe(1, listener);
        source.unsubscribe(1, listener);
        source.publish(1, 1, 2, 3, 4, 5);

        assertEquals(0, count[0]);
    }

    private static void copy(final Quote quote, final long[] captured) {
        captured[0] = quote.instrumentId();
        captured[1] = quote.bidPrice();
        captured[2] = quote.askPrice();
        captured[3] = quote.bidQuantity();
        captured[4] = quote.askQuantity();
        captured[5] = quote.epochNanos();
    }
}
