package com.nitroj.sor.core;

import com.nitroj.sor.api.MarketDataSnapshotSummary;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Quote;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Responsibility: verifies safe embedded engine diagnostics.
 *
 * <p>Role in system: proves the control plane exposes compact immutable
 * summaries instead of market-book or order-book internals.</p>
 *
 * <p>Relationships: composes {@link SorControlPlane}, engine-owned market
 * state, engine-owned order state, and reset evidence.</p>
 *
 * <p>Lifecycle: builds one engine per test and closes it after assertions.</p>
 *
 * <p>Design intent: make scenario notebooks and operators inspect state
 * without obtaining mutable hot-path data structures.</p>
 */
class SorEngineDiagnosticsTest {
    @Test
    void marketDataSnapshotReflectsEngineOwnedBookWithoutExposingArrays() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData);

        marketData.publish(0, 0, 100, 101, 10, 11, 123_456L);
        final MarketDataSnapshotSummary first = ((SorControlPlane) engine).marketDataSnapshot();
        marketData.publish(0, 0, 102, 103, 12, 13, 123_999L);
        final MarketDataSnapshotSummary second = ((SorControlPlane) engine).marketDataSnapshot();

        assertEquals(1, first.sequence());
        assertEquals(1, first.populatedCellCount());
        assertEquals(123_456L, first.lastUpdateEpochNanos());
        assertEquals(2, second.sequence());
        assertEquals(1, second.populatedCellCount());
        assertEquals(123_999L, second.lastUpdateEpochNanos());

        engine.close();
    }

    @Test
    void stateSummaryReportsOrderCountsPolicyAndLastResetEvidence() {
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData);
        final SorControlPlane controlPlane = engine;

        assertNull(controlPlane.stateSummary().lastResetSummary());

        engine.warmup(0);
        engine.submitParentOrder(ParentOrderRequest.builder()
                .instrumentId(0)
                .side(Side.BUY)
                .quantity(25)
                .urgency(0)
                .build());
        final SorStateSummary routed = controlPlane.stateSummary();

        assertEquals(1, routed.activeParentOrderCount());
        assertEquals(1, routed.activeChildOrderCount());
        assertEquals(25, routed.pendingChildQuantity());
        assertEquals(engine.activePolicy().version(), routed.activePolicyVersion());

        final SorResetSummary reset = controlPlane.reset(
                new SorResetRequest(SorResetMode.CLEAR_MARKET_DATA, false, "diagnostic test"));

        assertEquals(reset, controlPlane.stateSummary().lastResetSummary());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final RecordingMarketDataSource marketData) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1, true))
                .marketData(marketData)
                .venueAdapter(new EngineTestSupport.FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new EngineTestSupport.FakePersistence())
                .clock(new EngineTestSupport.ManualClock())
                .build();
    }

    /**
     * Responsibility: records venue-aware market-data subscriptions and emits
     * reusable quote carriers.
     *
     * <p>Role in system: drives the public market-data SPI path used by the
     * engine.</p>
     *
     * <p>Relationships: implements {@link MarketDataSource}.</p>
     *
     * <p>Lifecycle: constructed per test.</p>
     *
     * <p>Design intent: make diagnostic market snapshots deterministic.</p>
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
            listeners.get(key(instrumentId, venueId)).onQuote(
                    reusableQuote.set(instrumentId, venueId, bid, ask, bidQty, askQty, epochNanos));
        }

        private static long key(final int instrumentId, final int venueId) {
            return ((long) instrumentId << 32) ^ (venueId & 0xffff_ffffL);
        }
    }
}
