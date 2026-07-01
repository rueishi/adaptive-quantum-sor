package com.nitroj.sor.core;

import com.nitroj.sor.api.ChildOrderStateSnapshot;
import com.nitroj.sor.api.MarketDataSeedCell;
import com.nitroj.sor.api.MarketDataSeedSnapshot;
import com.nitroj.sor.api.OrderStateSnapshot;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.ParentOrderStateSnapshot;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorLifecycleEventTypes;
import com.nitroj.sor.api.SorNotReadyException;
import com.nitroj.sor.api.SorReadinessPhase;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorStartupHydrationRequest;
import com.nitroj.sor.api.SorStartupHydrationSummary;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies P9 startup hydration behavior in `SorEngineImpl`.
 *
 * <p>Role in system: proves the engine can hydrate OMS/EMS order snapshots and
 * market snapshots before accepting new parent intent.</p>
 *
 * <p>Relationships: composes public P9 hydration DTOs, `SorControlPlane`,
 * engine-owned `MarketBookState`, and package-local `EngineOrderBook`
 * diagnostics.</p>
 *
 * <p>Lifecycle: creates one embedded engine per test and closes it after
 * assertions.</p>
 *
 * <p>Design intent: keep the production startup contract executable without
 * depending on scenario/testkit classes.</p>
 */
class SorEngineStartupHydrationTest {
    @Test
    void hydrateLoadsMarketAndOrderSnapshotsBeforeAcceptingParentIntent() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final SorEngineImpl engine = buildEngine(new RecordingMarketDataSource(), persistence);
        final SorControlPlane controlPlane = engine;

        assertEquals(SorReadinessPhase.CONSTRUCTED, controlPlane.stateSummary().readinessPhase());
        final Throwable rejection = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> engine.submitParentOrder(request(10)));
        assertInstanceOf(SorNotReadyException.class, rejection);

        final SorStartupHydrationSummary summary = controlPlane.hydrate(hydrationRequest("orders-1", "market-1"));

        assertEquals(true, summary.accepted());
        assertEquals(true, engine.isReady());
        assertEquals(SorLifecycleEventTypes.HYDRATION_ACCEPTED,
                persistence.events.get(persistence.events.size() - 1).eventType());
        assertEquals(SorReadinessPhase.READY, controlPlane.stateSummary().readinessPhase());
        assertEquals(summary, controlPlane.stateSummary().lastHydrationSummary());
        assertEquals(1, controlPlane.stateSummary().activeParentOrderCount());
        assertEquals(1, controlPlane.stateSummary().activeChildOrderCount());
        assertEquals(300, controlPlane.stateSummary().pendingChildQuantity());
        assertEquals(1, controlPlane.marketDataSnapshot().populatedCellCount());
        assertEquals(100, engine.marketBookState().bidPriceTicks(0, 0));
        assertEquals(101, engine.marketBookState().askPriceTicks(0, 0));

        final Optional<OrderStatus> hydrated = engine.getOrderStatus(1);
        assertTrue(hydrated.isPresent());
        assertEquals(OrderStatusCode.ROUTED, hydrated.orElseThrow().status());
        assertEquals(800, hydrated.orElseThrow().remainingQuantity());

        final long newParentId = engine.submitParentOrder(request(10));
        assertEquals(2, newParentId);

        engine.close();
    }

    @Test
    void hydrateRejectsParentChildAggregateMismatchAndKeepsEngineNotReady() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final SorEngineImpl engine = buildEngine(new RecordingMarketDataSource(), persistence);

        final SorStartupHydrationSummary summary = ((SorControlPlane) engine).hydrate(
                hydrationRequest("orders-bad", "market-1", 301, 200));

        assertEquals(false, summary.accepted());
        assertEquals(SorReadinessPhase.RECOVERY_REQUIRED,
                ((SorControlPlane) engine).stateSummary().readinessPhase());
        assertEquals(false, engine.isReady());
        assertTrue(summary.failureReasons()[0].contains("pending child quantity"));
        assertEquals(SorLifecycleEventTypes.HYDRATION_REJECTED,
                persistence.events.get(persistence.events.size() - 1).eventType());

        engine.close();
    }

    @Test
    void sameAcceptedSnapshotIdIsIdempotent() {
        final SorEngineImpl engine = buildEngine(new RecordingMarketDataSource(), new RecordingPersistence());
        final SorControlPlane controlPlane = engine;
        final SorStartupHydrationRequest request = hydrationRequest("orders-1", "market-1");

        final SorStartupHydrationSummary first = controlPlane.hydrate(request);
        final SorStartupHydrationSummary second = controlPlane.hydrate(request);

        assertEquals(first, second);
        assertEquals(1, controlPlane.stateSummary().activeParentOrderCount());
        assertEquals(1, controlPlane.stateSummary().activeChildOrderCount());

        engine.close();
    }

    @Test
    void bufferedQuoteAfterResetIsAppliedOnceAfterHydration() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, new RecordingPersistence());
        final SorControlPlane controlPlane = engine;

        controlPlane.reset(new SorResetRequest(SorResetMode.PURGE_AND_REPOPULATE, false, "scenario"));
        assertEquals(SorReadinessPhase.MARKET_HYDRATING, controlPlane.stateSummary().readinessPhase());
        marketData.publish(0, 0, 102, 103, 12, 13, 300);

        final SorStartupHydrationSummary summary = controlPlane.hydrate(hydrationRequest("orders-1", "market-1"));

        assertEquals(true, summary.accepted());
        assertEquals(2, controlPlane.marketDataSnapshot().sequence());
        assertEquals(102, engine.marketBookState().bidPriceTicks(0, 0));
        assertEquals(300, controlPlane.marketDataSnapshot().lastUpdateEpochNanos());

        engine.close();
    }

    @Test
    void readinessPhaseCanReenterMarketHydratingAfterPurgeReset() {
        final SorEngineImpl engine = buildEngine(new RecordingMarketDataSource(), new RecordingPersistence());
        final SorControlPlane controlPlane = engine;

        controlPlane.hydrate(hydrationRequest("orders-1", "market-1"));
        assertEquals(SorReadinessPhase.READY, controlPlane.stateSummary().readinessPhase());
        controlPlane.reset(new SorResetRequest(SorResetMode.PURGE_AND_REPOPULATE, false, "scenario"));

        assertEquals(false, engine.isReady());
        assertEquals(SorReadinessPhase.MARKET_HYDRATING, controlPlane.stateSummary().readinessPhase());

        engine.close();
    }

    @Test
    void boundedLiveDeltaBufferOverflowFailsClosed() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, new RecordingPersistence());
        final SorControlPlane controlPlane = engine;

        controlPlane.reset(new SorResetRequest(SorResetMode.PURGE_AND_REPOPULATE, false, "scenario"));
        for (int i = 0; i < 1_024; i++) {
            marketData.publish(0, 0, 100 + i, 101 + i, 10, 11, 300 + i);
        }
        final IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> marketData.publish(0, 0, 2_000, 2_001, 10, 11, 2_000));

        assertTrue(ex.getMessage().contains("buffer overflow"));
        assertEquals(SorReadinessPhase.RECOVERY_REQUIRED, controlPlane.stateSummary().readinessPhase());
        assertEquals(false, engine.isReady());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final RecordingMarketDataSource marketData,
                                             final RecordingPersistence persistence) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1, true))
                .marketData(marketData)
                .venueAdapter(new EngineTestSupport.FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(persistence)
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

    private static SorStartupHydrationRequest hydrationRequest(final String orderSnapshotId,
                                                               final String marketSnapshotId) {
        return hydrationRequest(orderSnapshotId, marketSnapshotId, 300, 200);
    }

    private static SorStartupHydrationRequest hydrationRequest(final String orderSnapshotId,
                                                               final String marketSnapshotId,
                                                               final long parentPending,
                                                               final long parentFilled) {
        return new SorStartupHydrationRequest("hydrate-" + orderSnapshotId + '-' + marketSnapshotId,
                "all",
                new OrderStateSnapshot(orderSnapshotId, 10, 100,
                        new ParentOrderStateSnapshot[]{
                                new ParentOrderStateSnapshot(1, 0, Side.BUY, 101, 1_000,
                                        800, parentFilled, parentPending, OrderStatusCode.ROUTED, 110)
                        },
                        new ChildOrderStateSnapshot[]{
                                new ChildOrderStateSnapshot(11, 1, 0, 0, Side.BUY, 101,
                                        500, 300, 200, OrderStatusCode.ROUTED, 111)
                        }),
                new MarketDataSeedSnapshot(marketSnapshotId, 20, 200,
                        new MarketDataSeedCell[]{
                                new MarketDataSeedCell(0, 0, 100, 101, 10, 11, 200)
                        }),
                true,
                "startup");
    }

    private static final class RecordingMarketDataSource implements MarketDataSource {
        private final Map<Long, MarketDataListener> listeners = new ConcurrentHashMap<>();
        private final Quote quote = new Quote();

        @Override public void subscribe(final int instrumentId, final int venueId,
                                        final MarketDataListener listener) {
            listeners.put(key(instrumentId, venueId), listener);
        }
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {
            listeners.put(key(instrumentId, 0), listener);
        }
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
            listeners.values().remove(listener);
        }

        private void publish(final int instrumentId, final int venueId, final long bid, final long ask,
                             final long bidQty, final long askQty, final long epochNanos) {
            listeners.get(key(instrumentId, venueId)).onQuote(
                    quote.set(instrumentId, venueId, bid, ask, bidQty, askQty, epochNanos));
        }

        private static long key(final int instrumentId, final int venueId) {
            return ((long) instrumentId << 32) ^ (venueId & 0xffff_ffffL);
        }
    }

    private static final class RecordingPersistence implements Persistence {
        private final List<LifecycleEvent> events = new ArrayList<>();
        @Override public long appendLifecycleEvent(final LifecycleEvent event) {
            events.add(new LifecycleEvent().set(event.eventType(), event.subjectId(), event.epochNanos()));
            return events.size();
        }
        @Override public Iterator<LifecycleEvent> replay(final long fromEventIdExclusive) {
            return events.iterator();
        }
        @Override public void persistPolicySnapshot(final com.nitroj.sor.api.PolicyHandle handle,
                                                    final byte[] snapshotBytes) {
        }
        @Override public byte[] readPolicySnapshot(final long policyVersion) {
            return new byte[0];
        }
        @Override public long lastPersistedPolicyVersion() {
            return 0;
        }
    }
}
