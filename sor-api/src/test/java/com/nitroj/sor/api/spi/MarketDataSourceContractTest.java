package com.nitroj.sor.api.spi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Responsibility: verifies the market-data SPI subscription and reusable quote
 * semantics.
 *
 * <p>Role in system: proves a simulator or real feed can deliver quotes through
 * the public SPI without engine internals.</p>
 *
 * <p>Relationships: covers {@link MarketDataSource}, {@link MarketDataListener},
 * and {@link Quote} together.</p>
 *
 * <p>Lifecycle: uses a tiny fake source for unit-level contract validation.</p>
 *
 * <p>Design intent: make quote reuse explicit so listeners copy fields instead
 * of retaining mutable carriers.</p>
 */
class MarketDataSourceContractTest {
    /**
     * Subscribes, receives quotes, observes object reuse, then unsubscribes.
     */
    @Test
    void subscribeUnsubscribeAndQuoteReuseWork() {
        final FakeSource source = new FakeSource();
        final List<Long> copiedBids = new ArrayList<>();
        final List<Quote> retainedRefs = new ArrayList<>();
        final MarketDataListener listener = quote -> {
            copiedBids.add(quote.bidPrice());
            retainedRefs.add(quote);
        };

        source.subscribe(1, listener);
        source.publish(10);
        source.publish(11);
        source.unsubscribe(1, listener);
        source.publish(12);

        assertEquals(List.of(10L, 11L), copiedBids);
        assertEquals(2, retainedRefs.size());
        assertSame(retainedRefs.get(0), retainedRefs.get(1), "source reuses one quote instance");
        assertEquals(12, retainedRefs.get(0).bidPrice(), "retaining quote reference observes later mutation");
    }

    private static final class FakeSource implements MarketDataSource {
        private final Quote quote = new Quote();
        private MarketDataListener listener;

        @Override
        public void subscribe(final int instrumentId, final MarketDataListener listener) {
            this.listener = listener;
        }

        @Override
        public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
            if (this.listener == listener) {
                this.listener = null;
            }
        }

        void publish(final long bid) {
            quote.set(1, bid, bid + 1, 100, 200, bid);
            if (listener != null) {
                listener.onQuote(quote);
            }
        }
    }
}
