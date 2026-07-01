package com.nitroj.sor.api;

import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: documents Phase 9 API/SPI compatibility.
 *
 * <p>Role in system: proves venue-aware market-data and reset configuration
 * additions preserve the legacy constructor/method shapes used by existing
 * embedders.</p>
 *
 * <p>Relationships: covers {@link Quote}, {@link MarketDataSource}, and
 * {@link SorConfig} compatibility behavior.</p>
 *
 * <p>Lifecycle: run by `:sor-api:test` as the Phase 9 compatibility gate.</p>
 *
 * <p>Design intent: make API/SPI breaks deliberate and documented.</p>
 */
class Phase9ApiSpiCompatibilityTest {
    @Test
    void legacyQuoteSetterDefaultsVenueToZero() {
        final Quote quote = new Quote().set(2, 100, 101, 10, 11, 1_000);

        assertEquals(2, quote.instrumentId());
        assertEquals(0, quote.venueId());
        assertEquals(100, quote.bidPrice());
    }

    @Test
    void venueAwareMarketDataDefaultMethodsDelegateToLegacySubscription() {
        final LegacyOnlyMarketDataSource source = new LegacyOnlyMarketDataSource();
        final MarketDataListener listener = quote -> {};

        source.subscribe(3, 4, listener);
        source.unsubscribe(3, 4, listener);

        assertEquals(3, source.subscribedInstrumentId);
        assertEquals(listener, source.listener);
        assertEquals(3, source.unsubscribedInstrumentId);
    }

    @Test
    void legacySorConfigConstructorKeepsProductionSafeResetDefault() {
        final SorConfig config = new SorConfig(8, 9);

        assertEquals(1, config.instrumentCount());
        assertEquals(1, config.venueCount());
        assertEquals(false, config.destructiveResetEnabled());
    }

    private static final class LegacyOnlyMarketDataSource implements MarketDataSource {
        private int subscribedInstrumentId;
        private int unsubscribedInstrumentId;
        private MarketDataListener listener;

        @Override
        public void subscribe(final int instrumentId, final MarketDataListener listener) {
            this.subscribedInstrumentId = instrumentId;
            this.listener = listener;
        }

        @Override
        public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
            this.unsubscribedInstrumentId = instrumentId;
        }
    }
}
