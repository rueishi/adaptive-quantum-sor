package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.RejectReport;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.VenueAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: verifies `SorEngineImpl` uses its engine-owned order book for
 * public parent-order state.
 *
 * <p>Role in system: bridges the local {@link EngineOrderBook} to public
 * `getOrderStatus` behavior.</p>
 *
 * <p>Relationships: composes `SorEngineBuilder`, a tiny market-data source,
 * venue callbacks, and public order status DTOs.</p>
 *
 * <p>Lifecycle: builds one engine per test, warms it up, submits a parent
 * order, emits venue callbacks, and closes the engine.</p>
 *
 * <p>Design intent: prove OMS/EMS-facing status reads reflect engine-owned
 * working state rather than only the original accepted status.</p>
 */
class SorEngineImplOrderBookTest {
    /**
     * Confirms submitting a parent creates local order-book state and public
     * accepted status.
     */
    @Test
    void submitCreatesEngineOwnedParentState() {
        final CapturingVenueAdapter venue = new CapturingVenueAdapter();
        final SorEngineImpl engine = buildEngine(venue);
        engine.warmup(0);

        final long parentOrderId = engine.submitParentOrder(request(100));

        assertEquals(100, engine.orderBook().parent(parentOrderId).remainingQuantity());
        assertEquals(OrderStatusCode.ACCEPTED, engine.getOrderStatus(parentOrderId).orElseThrow().status());

        engine.close();
    }

    /**
     * Confirms venue fills update engine-owned order state and public status.
     */
    @Test
    void fillCallbackUpdatesPublicOrderStatus() {
        final CapturingVenueAdapter venue = new CapturingVenueAdapter();
        final SorEngineImpl engine = buildEngine(venue);
        engine.warmup(0);
        final long parentOrderId = engine.submitParentOrder(request(100));

        venue.fill(new FillReport().set(10, parentOrderId, 0, 40, 101, 5_000));

        final OrderStatus status = engine.getOrderStatus(parentOrderId).orElseThrow();
        assertEquals(OrderStatusCode.PARTIALLY_FILLED, status.status());
        assertEquals(40, status.filledQuantity());
        assertEquals(60, status.remainingQuantity());

        engine.close();
    }

    /**
     * Confirms venue rejects update engine-owned order state and public status.
     */
    @Test
    void rejectCallbackUpdatesPublicOrderStatus() {
        final CapturingVenueAdapter venue = new CapturingVenueAdapter();
        final SorEngineImpl engine = buildEngine(venue);
        engine.warmup(0);
        final long parentOrderId = engine.submitParentOrder(request(100));

        venue.reject(new RejectReport().set(10, parentOrderId, 0, 7, 5_000));

        final OrderStatus status = engine.getOrderStatus(parentOrderId).orElseThrow();
        assertEquals(OrderStatusCode.REJECTED, status.status());
        assertEquals(0, status.filledQuantity());
        assertEquals(100, status.remainingQuantity());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final CapturingVenueAdapter venue) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1))
                .marketData(new NoopMarketData())
                .venueAdapter(venue)
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new EngineTestSupport.FakePersistence())
                .clock(new EngineTestSupport.ManualClock())
                .build();
    }

    private static ParentOrderRequest request(final long quantity) {
        return ParentOrderRequest.builder()
                .instrumentId(0)
                .side(Side.BUY)
                .quantity(quantity)
                .urgency(0)
                .build();
    }

    /**
     * Responsibility: no-op market-data source for order-state tests.
     *
     * <p>Role in system: satisfies the engine builder without influencing
     * order-book assertions.</p>
     *
     * <p>Relationships: implements {@link MarketDataSource} and accepts the
     * engine's venue-aware subscriptions.</p>
     *
     * <p>Lifecycle: constructed per engine and discarded after close.</p>
     *
     * <p>Design intent: keep order-state tests independent from market-data
     * ingestion behavior.</p>
     */
    private static final class NoopMarketData implements MarketDataSource {
        @Override public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {}
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
    }

    /**
     * Responsibility: captures the engine venue callback and exposes explicit
     * fill/reject triggers.
     *
     * <p>Role in system: simulates venue reports without implementing child
     * order emission, which is owned by a later Phase 9 card.</p>
     *
     * <p>Relationships: implements {@link VenueAdapter} for `SorEngineImpl`
     * construction.</p>
     *
     * <p>Lifecycle: constructed per test and receives its callback during
     * engine build.</p>
     *
     * <p>Design intent: keep callback-driven order-state tests small and
     * deterministic.</p>
     */
    private static final class CapturingVenueAdapter implements VenueAdapter {
        private VenueAdapterCallback callback;

        @Override
        public RingWriter childOrderRingWriter(final int venueId) {
            return childOrder -> true;
        }

        @Override
        public void callback(final VenueAdapterCallback callback) {
            this.callback = callback;
        }

        void fill(final FillReport report) {
            callback.deliverFill(report);
        }

        void reject(final RejectReport report) {
            callback.deliverReject(report);
        }
    }
}
