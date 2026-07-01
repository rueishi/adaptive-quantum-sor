package com.nitroj.sor.core;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.SorLifecycleEventTypes;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: verifies embedded reset control-plane behavior.
 *
 * <p>Role in system: proves reset is exposed through {@link SorControlPlane},
 * not as a hot-path `SorEngine` method, and that destructive resets are
 * safety-gated.</p>
 *
 * <p>Relationships: composes public reset DTOs, `SorEngineImpl`,
 * `MarketBookState`, `EngineOrderBook`, and persistence lifecycle evidence.</p>
 *
 * <p>Lifecycle: builds one engine per test, invokes reset, then closes the
 * engine.</p>
 *
 * <p>Design intent: make purge behavior explicit and auditable before scenario
 * replay uses it.</p>
 */
class SorEngineResetControlPlaneTest {
    /**
     * Confirms production-style config rejects destructive reset modes and
     * records lifecycle evidence.
     */
    @Test
    void destructiveResetIsRejectedWhenConfigDisablesIt() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final SorEngineImpl engine = buildEngine(false, persistence);

        final SorResetSummary summary = ((SorControlPlane) engine).reset(
                new SorResetRequest(SorResetMode.CLEAR_MARKET_DATA, true, "operator requested"));

        assertEquals(false, summary.accepted());
        assertEquals("destructive reset disabled by configuration", summary.message());
        assertEquals(1, persistence.events.size());

        engine.close();
    }

    /**
     * Confirms reset fails closed when live orders exist and the request
     * requires quiescence.
     */
    @Test
    void destructiveResetFailsClosedWhenLiveOrdersExist() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final SorEngineImpl engine = buildEngine(true, persistence);
        engine.warmup(0);
        engine.submitParentOrder(ParentOrderRequest.builder()
                .instrumentId(0)
                .side(Side.BUY)
                .quantity(100)
                .urgency(0)
                .build());

        final SorResetSummary summary = ((SorControlPlane) engine).reset(
                new SorResetRequest(SorResetMode.PURGE_RUNTIME_STATE, true, "scenario reset"));

        assertEquals(false, summary.accepted());
        assertEquals("live orders present; reset rejected", summary.message());
        assertEquals(SorLifecycleEventTypes.RESET_REJECTED,
                persistence.events.get(persistence.events.size() - 1).eventType());

        engine.close();
    }

    /**
     * Confirms accepted clear-market-data reset empties the engine-owned market
     * book and reports cleared/kept state.
     */
    @Test
    void clearMarketDataResetClearsMarketBookAndReportsEvidence() {
        final RecordingPersistence persistence = new RecordingPersistence();
        final SorEngineImpl engine = buildEngine(true, persistence);
        engine.marketBookState().updateTopOfBook(0, 0, 100, 101, 1_000, 2_000);

        final SorResetSummary summary = ((SorControlPlane) engine).reset(
                new SorResetRequest(SorResetMode.CLEAR_MARKET_DATA, true, "scenario reset"));

        assertEquals(true, summary.accepted());
        assertArrayEquals(new String[]{"marketBook"}, summary.clearedState());
        assertArrayEquals(new String[]{"activePolicy"}, summary.keptState());
        assertEquals(0, engine.marketBookState().sequence());
        assertEquals(1, persistence.events.size());

        engine.close();
    }

    private static SorEngineImpl buildEngine(final boolean destructiveResetEnabled,
                                             final RecordingPersistence persistence) {
        return (SorEngineImpl) SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000, 1, 1, destructiveResetEnabled))
                .marketData(new NoopMarketData())
                .venueAdapter(new EngineTestSupport.FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(persistence)
                .clock(new EngineTestSupport.ManualClock())
                .build();
    }

    /**
     * Responsibility: no-op market-data source for reset tests.
     *
     * <p>Role in system: satisfies engine construction without publishing
     * quotes.</p>
     *
     * <p>Relationships: implements {@link MarketDataSource}.</p>
     *
     * <p>Lifecycle: constructed per test and discarded with the engine.</p>
     *
     * <p>Design intent: isolate reset behavior from market-data publishing.</p>
     */
    private static final class NoopMarketData implements MarketDataSource {
        @Override public void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {}
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
    }

    /**
     * Responsibility: records lifecycle events appended by reset operations.
     *
     * <p>Role in system: verifies reset attempts are auditable.</p>
     *
     * <p>Relationships: implements {@link Persistence} for `SorEngineImpl`
     * tests.</p>
     *
     * <p>Lifecycle: created per test and read after reset.</p>
     *
     * <p>Design intent: avoid filesystem persistence while proving the SPI call
     * occurs.</p>
     */
    private static final class RecordingPersistence implements Persistence {
        private final List<LifecycleEvent> events = new ArrayList<>();

        @Override
        public long appendLifecycleEvent(final LifecycleEvent event) {
            event.sequenceNumber(events.size() + 1L);
            events.add(event);
            return event.sequenceNumber();
        }

        @Override public void persistPolicySnapshot(final com.nitroj.sor.api.PolicyHandle handle, final byte[] snapshotBytes) {}
        @Override public byte[] readPolicySnapshot(final long policyVersion) { return new byte[0]; }
        @Override public long lastPersistedPolicyVersion() { return 0; }
        @Override public Iterator<LifecycleEvent> replay(final long sinceSequence) { return events.iterator(); }
    }
}
