package com.nitroj.sor.core;

import com.nitroj.sor.api.BackpressureException;
import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.VenueAdapter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the engine emits child orders through the public
 * venue adapter SPI.
 *
 * <p>Role in system: proves Phase 9 venue integration starts at the real
 * `VenueAdapter.childOrderRingWriter` boundary instead of simulator-local
 * direct calls.</p>
 *
 * <p>Relationships: composes `SorEngineImpl`, {@link VenueAdapter},
 * {@link RingWriter}, order-book state, observability, and public events.</p>
 *
 * <p>Lifecycle: builds a fresh engine per test, warms it up, submits one order,
 * and closes it.</p>
 *
 * <p>Design intent: keep routing deliberately minimal while proving the venue
 * handoff and backpressure contract.</p>
 */
class SorEngineVenueAdapterIntegrationTest {
    /**
     * Confirms submit emits a child order to the venue ring and tracks pending
     * child quantity in the engine-owned order book.
     */
    @Test
    void submitOffersChildOrderToVenueRing() {
        final CapturingRing ring = new CapturingRing(true);
        final SorEngineImpl engine = buildEngine(ring, Observability.noop());
        engine.warmup(0);

        final long parentOrderId = engine.submitParentOrder(request(50));

        assertEquals(parentOrderId, ring.last.parentOrderId());
        assertEquals(0, ring.last.venueId());
        assertEquals(50, ring.last.quantity());
        assertEquals(50, engine.orderBook().parent(parentOrderId).pendingChildQuantity());
        assertEquals(OrderStatusCode.ROUTED, engine.orderBook().child(ring.last.childOrderId()).status());

        engine.close();
    }

    /**
     * Confirms a full venue ring records child-order backpressure and emits a
     * backpressure event.
     */
    @Test
    void fullVenueRingRecordsBackpressureAndEvent() throws InterruptedException {
        final CapturingRing ring = new CapturingRing(false);
        final CountingObservability observability = new CountingObservability();
        final SorEngineImpl engine = buildEngine(ring, observability);
        final CountDownLatch latch = new CountDownLatch(1);
        engine.registerListener(event -> {
            if (event instanceof SorEvent.BackpressureRejected) {
                latch.countDown();
            }
        });
        engine.warmup(0);

        assertThrows(BackpressureException.class, () -> engine.submitParentOrder(request(50)));

        assertEquals(1, observability.backpressureCount.get());
        assertTrue(latch.await(1, TimeUnit.SECONDS), "backpressure event should be emitted");

        engine.close();
    }

    private static SorEngineImpl buildEngine(final RingWriter ring, final Observability observability) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1))
                .marketData(new NoopMarketData())
                .venueAdapter(new SingleRingVenueAdapter(ring))
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new EngineTestSupport.FakePersistence())
                .clock(new EngineTestSupport.ManualClock())
                .observability(observability)
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
     * Responsibility: no-op market-data source for venue integration tests.
     *
     * <p>Role in system: satisfies engine construction while keeping the test
     * focused on child-order venue output.</p>
     *
     * <p>Relationships: implements {@link MarketDataSource}.</p>
     *
     * <p>Lifecycle: constructed per engine.</p>
     *
     * <p>Design intent: avoid market-data side effects in venue tests.</p>
     */
    private static final class NoopMarketData implements MarketDataSource {
        @Override public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {}
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
    }

    /**
     * Responsibility: returns one configured ring for every venue.
     *
     * <p>Role in system: lets tests control venue-ring acceptance or
     * backpressure.</p>
     *
     * <p>Relationships: implements {@link VenueAdapter}.</p>
     *
     * <p>Lifecycle: constructed before engine build.</p>
     *
     * <p>Design intent: isolate ring offer behavior from simulator logic.</p>
     */
    private static final class SingleRingVenueAdapter implements VenueAdapter {
        private final RingWriter ring;

        private SingleRingVenueAdapter(final RingWriter ring) {
            this.ring = ring;
        }

        @Override public RingWriter childOrderRingWriter(final int venueId) { return ring; }
        @Override public void callback(final VenueAdapterCallback callback) {}
    }

    /**
     * Responsibility: captures the last offered child order and optionally
     * rejects offers.
     *
     * <p>Role in system: test double for venue ring backpressure.</p>
     *
     * <p>Relationships: implements {@link RingWriter}.</p>
     *
     * <p>Lifecycle: constructed per test.</p>
     *
     * <p>Design intent: make offered child order fields observable without
     * retaining the mutable carrier.</p>
     */
    private static final class CapturingRing implements RingWriter {
        private final boolean accept;
        private ChildOrderRef last;

        private CapturingRing(final boolean accept) {
            this.accept = accept;
        }

        @Override
        public boolean offer(final ChildOrderRef childOrder) {
            last = new ChildOrderRef().set(childOrder.childOrderId(), childOrder.parentOrderId(),
                    childOrder.venueId(), childOrder.side(), childOrder.quantity(), childOrder.limitPrice(),
                    childOrder.createdEpochNanos());
            return accept;
        }
    }

    /**
     * Responsibility: counts backpressure observations.
     *
     * <p>Role in system: verifies venue-ring backpressure reaches
     * observability.</p>
     *
     * <p>Relationships: implements {@link Observability} directly because only
     * the backpressure method matters.</p>
     *
     * <p>Lifecycle: constructed per test.</p>
     *
     * <p>Design intent: avoid pulling in the full observability module for a
     * core unit test.</p>
     */
    private static final class CountingObservability implements Observability {
        private final AtomicInteger backpressureCount = new AtomicInteger();

        @Override public void recordRouteDecisionLatency(final long nanos) {}
        @Override public void recordParentOrderRingDepth(final int depth) {}
        @Override public void recordBackpressureRejected(final int reasonCode) { backpressureCount.incrementAndGet(); }
        @Override public void recordPolicyPublished(final long policyVersion, final long policyHash64, final long durationNanos) {}
    }
}
