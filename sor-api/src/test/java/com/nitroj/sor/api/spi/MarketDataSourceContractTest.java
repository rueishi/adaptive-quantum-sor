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
 * <p>Design intent: make quote reuse and venue-aware delivery explicit so
 * listeners copy fields instead of retaining mutable carriers.</p>
 */
class MarketDataSourceContractTest {
    /**
     * Subscribes, receives quotes, observes object reuse, then unsubscribes.
     */
    @Test
    void subscribeUnsubscribeAndQuoteReuseWork() {
        final FakeSource source = new FakeSource();
        final List<Long> copiedBids = new ArrayList<>();
        final List<Integer> copiedVenues = new ArrayList<>();
        final List<Quote> retainedRefs = new ArrayList<>();
        final MarketDataListener listener = quote -> {
            copiedBids.add(quote.bidPrice());
            copiedVenues.add(quote.venueId());
            retainedRefs.add(quote);
        };

        source.subscribe(1, 2, listener);
        source.publish(2, 10);
        source.publish(2, 11);
        source.unsubscribe(1, 2, listener);
        source.publish(2, 12);

        assertEquals(List.of(10L, 11L), copiedBids);
        assertEquals(List.of(2, 2), copiedVenues);
        assertEquals(2, retainedRefs.size());
        assertSame(retainedRefs.get(0), retainedRefs.get(1), "source reuses one quote instance");
        assertEquals(12, retainedRefs.get(0).bidPrice(), "retaining quote reference observes later mutation");
    }

    /**
     * Confirms the instrument-only subscription remains a wildcard for
     * migration while the quote still carries venue identity.
     */
    @Test
    void instrumentOnlySubscriptionReceivesEveryVenueAsWildcard() {
        final FakeSource source = new FakeSource();
        final List<Integer> venues = new ArrayList<>();

        source.subscribe(1, quote -> venues.add(quote.venueId()));
        source.publish(2, 10);
        source.publish(3, 11);

        assertEquals(List.of(2, 3), venues);
    }

    private static final class FakeSource implements MarketDataSource {
        private final Quote quote = new Quote();
        private MarketDataListener wildcardListener;
        private MarketDataListener venueListener;
        private int venueId;

        @Override
        public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
            this.venueId = venueId;
            this.venueListener = listener;
        }

        @Override
        public void subscribe(final int instrumentId, final MarketDataListener listener) {
            this.wildcardListener = listener;
        }

        @Override
        public void unsubscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
            if (this.venueId == venueId && this.venueListener == listener) {
                this.venueListener = null;
            }
        }

        @Override
        public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
            if (this.wildcardListener == listener) {
                this.wildcardListener = null;
            }
        }

        void publish(final int venueId, final long bid) {
            quote.set(1, venueId, bid, bid + 1, 100, 200, bid);
            if (venueListener != null && this.venueId == venueId) {
                venueListener.onQuote(quote);
            }
            if (wildcardListener != null) {
                wildcardListener.onQuote(quote);
            }
        }
    }
}
