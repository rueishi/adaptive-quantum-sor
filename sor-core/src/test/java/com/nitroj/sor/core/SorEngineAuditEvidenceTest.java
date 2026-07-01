package com.nitroj.sor.core;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorLifecycleEventTypes;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
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
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies lifecycle audit evidence emitted by the embedded
 * engine.
 *
 * <p>Role in system: proves reset, policy publication, route/child emission,
 * venue reports, and rejected market data are replayable through the
 * persistence SPI.</p>
 *
 * <p>Relationships: composes {@link SorEngineImpl}, {@link Persistence}, and
 * public {@link SorLifecycleEventTypes}.</p>
 *
 * <p>Lifecycle: builds one engine per test and replays captured events.</p>
 *
 * <p>Design intent: keep audit checks primitive and deterministic.</p>
 */
class SorEngineAuditEvidenceTest {
    @Test
    void lifecycleReplayIncludesResetPolicyRouteChildAndFillEvidence() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final EngineTestSupport.FakeVenueAdapter venueAdapter = new EngineTestSupport.FakeVenueAdapter();
        final SorEngineImpl engine = buildEngine(new NoopMarketData(), venueAdapter, persistence);

        ((SorControlPlane) engine).reset(new SorResetRequest(
                SorResetMode.CLEAR_MARKET_DATA, false, "audit test"));
        engine.warmup(0);
        engine.submitParentOrder(ParentOrderRequest.builder()
                .instrumentId(0)
                .side(Side.BUY)
                .quantity(10)
                .urgency(0)
                .build());
        venueAdapter.fill();

        final List<Long> eventTypes = persistence.eventTypes();
        assertTrue(eventTypes.contains(SorLifecycleEventTypes.RESET_ACCEPTED));
        assertTrue(eventTypes.contains(SorLifecycleEventTypes.POLICY_PUBLISHED));
        assertTrue(eventTypes.contains(SorLifecycleEventTypes.CHILD_ORDER_EMITTED));
        assertTrue(eventTypes.contains(SorLifecycleEventTypes.ROUTE_DECIDED));
        assertTrue(eventTypes.contains(SorLifecycleEventTypes.FILL_DELIVERED));

        engine.close();
    }

    @Test
    void rejectedQuoteIsVisibleInLifecycleReplay() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final RecordingMarketDataSource marketData = new RecordingMarketDataSource();
        final SorEngineImpl engine = buildEngine(marketData, new EngineTestSupport.FakeVenueAdapter(), persistence);

        assertThrows(IllegalArgumentException.class,
                () -> marketData.publish(0, 0, 101, 100, 10, 11, 222L));

        assertEquals(SorLifecycleEventTypes.MARKET_DATA_REJECTED, persistence.events.get(0).eventType());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final MarketDataSource marketData,
                                             final EngineTestSupport.FakeVenueAdapter venueAdapter,
                                             final Persistence persistence) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1, true))
                .marketData(marketData)
                .venueAdapter(venueAdapter)
                .riskProvider((request, decision) -> decision.allow())
                .persistence(persistence)
                .clock(new EngineTestSupport.ManualClock())
                .build();
    }

    private static final class NoopMarketData implements MarketDataSource {
        @Override public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {}
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
    }

    private static final class RecordingMarketDataSource implements MarketDataSource {
        private final Map<Long, MarketDataListener> listeners = new ConcurrentHashMap<>();
        private final Quote reusableQuote = new Quote();

        @Override
        public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
            listeners.put(key(instrumentId, venueId), listener);
        }

        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}

        void publish(final int instrumentId, final int venueId, final long bid, final long ask,
                     final long bidQty, final long askQty, final long epochNanos) {
            listeners.get(key(instrumentId, venueId)).onQuote(
                    reusableQuote.set(instrumentId, venueId, bid, ask, bidQty, askQty, epochNanos));
        }

        private static long key(final int instrumentId, final int venueId) {
            return ((long) instrumentId << 32) ^ (venueId & 0xffff_ffffL);
        }
    }

    private static final class RecordingPersistence implements Persistence {
        private final List<LifecycleEvent> events = new ArrayList<>();

        @Override
        public long appendLifecycleEvent(final LifecycleEvent event) {
            final LifecycleEvent copy = new LifecycleEvent()
                    .set(event.eventType(), event.subjectId(), event.epochNanos())
                    .sequenceNumber(events.size() + 1L);
            events.add(copy);
            event.sequenceNumber(copy.sequenceNumber());
            return copy.sequenceNumber();
        }

        List<Long> eventTypes() {
            return events.stream().map(LifecycleEvent::eventType).toList();
        }

        @Override public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {}
        @Override public byte[] readPolicySnapshot(final long policyVersion) { return new byte[0]; }
        @Override public long lastPersistedPolicyVersion() { return 0; }
        @Override public Iterator<LifecycleEvent> replay(final long sinceSequence) { return events.iterator(); }
    }
}
