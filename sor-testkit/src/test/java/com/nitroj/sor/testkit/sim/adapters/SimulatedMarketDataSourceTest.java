package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: verifies the simulator market-data source implements the
 * public venue-aware market-data SPI.
 *
 * <p>Role in system: proves scenario-generated quotes carry instrument and
 * venue identity through the same listener contract used by the engine.</p>
 *
 * <p>Relationships: covers {@link SimulatedMarketDataSource} with
 * {@link Quote} copies, leaving simulator book formula coverage to dedicated
 * scenario tests.</p>
 *
 * <p>Lifecycle: unit-level test for the `sor-test-server` simulator adapter
 * package.</p>
 *
 * <p>Design intent: preserve deterministic simulator control while making
 * venue-aware quote delivery observable.</p>
 */
class SimulatedMarketDataSourceTest {
    /**
     * Confirms exact venue subscriptions receive the venue identity published
     * by the simulator source.
     */
    @Test
    void subscribedListenerReceivesDeterministicQuoteFields() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final long[] captured = new long[7];

        source.subscribe(7, 2, quote -> copy(quote, captured));
        source.publish(7, 2, 101, 103, 10, 11, 12345);

        assertArrayEquals(new long[] {7, 2, 101, 103, 10, 11, 12345}, captured);
    }

    /**
     * Confirms instrument-only subscriptions remain wildcard subscriptions
     * while each delivered quote still identifies its originating venue.
     */
    @Test
    void instrumentOnlySubscriptionReceivesAllVenuesAsWildcard() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final long[] venues = {-1, -1};

        source.subscribe(7, quote -> venues[(int) quote.venueId() - 1] = quote.venueId());
        source.publish(7, 1, 101, 103, 10, 11, 12345);
        source.publish(7, 2, 102, 104, 12, 13, 12346);

        assertArrayEquals(new long[] {1, 2}, venues);
    }

    /**
     * Confirms exact venue subscriptions do not receive unrelated venue
     * updates for the same instrument.
     */
    @Test
    void venueSubscriptionReceivesOnlyMatchingVenue() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final int[] count = {0};

        source.subscribe(1, 2, quote -> count[0]++);
        source.publish(1, 1, 1, 2, 3, 4, 5);
        source.publish(1, 2, 1, 2, 3, 4, 5);

        assertEquals(1, count[0]);
    }

    /**
     * Confirms venue-aware unsubscription stops exact venue delivery.
     */
    @Test
    void unsubscribeStopsDelivery() {
        final SimulatedMarketDataSource source = new SimulatedMarketDataSource();
        final int[] count = {0};
        final com.nitroj.sor.api.spi.MarketDataListener listener = quote -> count[0]++;

        source.subscribe(1, 2, listener);
        source.unsubscribe(1, 2, listener);
        source.publish(1, 2, 1, 2, 3, 4, 5);

        assertEquals(0, count[0]);
    }

    private static void copy(final Quote quote, final long[] captured) {
        captured[0] = quote.instrumentId();
        captured[1] = quote.venueId();
        captured[2] = quote.bidPrice();
        captured[3] = quote.askPrice();
        captured[4] = quote.bidQuantity();
        captured[5] = quote.askQuantity();
        captured[6] = quote.epochNanos();
    }
}
