package com.nitroj.sor.core;

import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verifies `SorEngineImpl` copies venue-aware market data into
 * its own market book.
 *
 * <p>Role in system: proves the Phase 9 ingestion path no longer stops at an
 * empty `onQuote` callback and does not route from simulator-owned market
 * state.</p>
 *
 * <p>Relationships: composes public {@link MarketDataSource} callbacks,
 * {@link Quote} carriers, `SorEngineBuilder`, and package-local
 * `SorEngineImpl` diagnostics.</p>
 *
 * <p>Lifecycle: unit-level core test that builds an engine, publishes quotes
 * through the configured SPI source, and closes the engine after assertions.</p>
 *
 * <p>Design intent: make engine-owned market-book state explicit before later
 * Phase 9 cards route and reset against it.</p>
 */
class SorEngineMarketDataIngestionTest {
    /**
     * Confirms venue-aware quotes update the matching instrument/venue cell in
     * the engine-owned market book.
     */
    @Test
    void onQuoteUpdatesMatchingMarketBookCell() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, 2, 3);

        marketData.publish(1, 2, 100, 101, 10, 11, 1_000);

        assertEquals(100, engine.marketBookState().bidPriceTicks(1, 2));
        assertEquals(101, engine.marketBookState().askPriceTicks(1, 2));
        assertEquals(10, engine.marketBookState().bidQty(1, 2));
        assertEquals(11, engine.marketBookState().askQty(1, 2));
        assertEquals(1, engine.marketBookState().sequence());

        engine.close();
    }

    /**
     * Confirms the engine copies primitive quote fields and is not affected by
     * later mutation of the source-owned reusable quote.
     */
    @Test
    void marketBookRetainsCopiedFieldsWhenReusableQuoteMutates() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, 1, 1);

        marketData.publish(0, 0, 100, 101, 10, 11, 1_000);
        marketData.reusableQuote.set(0, 0, 200, 201, 20, 21, 2_000);

        assertEquals(100, engine.marketBookState().bidPriceTicks(0, 0));
        assertEquals(101, engine.marketBookState().askPriceTicks(0, 0));
        assertEquals(1, engine.marketBookState().sequence());

        engine.close();
    }

    /**
     * Confirms invalid crossed quote data is rejected by market-book
     * validation and does not advance the market-book sequence.
     */
    @Test
    void invalidQuoteIsRejectedBeforeStateMutation() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, 1, 1);

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> marketData.publish(0, 0, 101, 100, 10, 11, 1_000));

        assertEquals("bid must be less than ask", exception.getMessage());
        assertEquals(0, engine.marketBookState().sequence());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final MarketDataSource marketData,
                                             final int instruments,
                                             final int venues) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, instruments, venues))
                .marketData(marketData)
                .venueAdapter(new EngineTestSupport.FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new EngineTestSupport.FakePersistence())
                .clock(new EngineTestSupport.ManualClock())
                .build();
    }

    /**
     * Responsibility: tiny market-data source that records venue-aware
     * subscriptions and publishes through a reusable quote carrier.
     *
     * <p>Role in system: isolates engine ingestion tests from simulator-local
     * book state.</p>
     *
     * <p>Relationships: implements the public {@link MarketDataSource} SPI and
     * invokes engine-owned {@link MarketDataListener} callbacks.</p>
     *
     * <p>Lifecycle: constructed per test, subscribed during engine build, then
     * used for explicit publishes.</p>
     *
     * <p>Design intent: make callback behavior deterministic and allocation
     * irrelevant to the assertions.</p>
     */
    private static final class RecordingMarketDataSource implements MarketDataSource {
        private final Map<Long, MarketDataListener> listeners = new ConcurrentHashMap<>();
        private final Quote reusableQuote = new Quote();

        @Override
        public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
            listeners.put(key(instrumentId, venueId), listener);
        }

        @Override
        public void subscribe(final int instrumentId, final MarketDataListener listener) {
            throw new AssertionError("engine should use venue-aware subscriptions");
        }

        @Override
        public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
            listeners.values().remove(listener);
        }

        void publish(final int instrumentId, final int venueId, final long bid, final long ask,
                     final long bidQty, final long askQty, final long epochNanos) {
            final MarketDataListener listener = listeners.get(key(instrumentId, venueId));
            if (listener == null) {
                throw new AssertionError("missing listener for instrument=" + instrumentId + ", venue=" + venueId);
            }
            listener.onQuote(reusableQuote.set(instrumentId, venueId, bid, ask, bidQty, askQty, epochNanos));
        }

        private static long key(final int instrumentId, final int venueId) {
            return ((long) instrumentId << 32) ^ (venueId & 0xffff_ffffL);
        }
    }
}
