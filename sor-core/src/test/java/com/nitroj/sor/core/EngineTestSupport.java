package com.nitroj.sor.core;

import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.RejectReport;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.RiskDecision;
import com.nitroj.sor.api.spi.RiskProvider;
import com.nitroj.sor.api.spi.VenueAdapter;
import com.nitroj.sor.api.PolicyHandle;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Test fixtures for P8-06 engine implementation tests. */
final class EngineTestSupport {
    private EngineTestSupport() {}

    static SorEngine engine() {
        return builder().build();
    }

    static SorEngineBuilder builder() {
        return SorEngineBuilder.create()
                .config(new SorConfig(1024, 1000))
                .marketData(new FakeMarketData())
                .venueAdapter(new FakeVenueAdapter())
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new FakePersistence())
                .clock(new ManualClock());
    }

    static class ManualClock implements Clock {
        private final AtomicLong now = new AtomicLong(1_000_000L);
        @Override public long nanoTime() { return now.addAndGet(1_000L); }
        @Override public long epochNanos() { return now.addAndGet(1_000L); }
        void advance(final long nanos) { now.addAndGet(nanos); }
    }

    static final class FakeMarketData implements MarketDataSource {
        @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
        @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
    }

    static final class FakeVenueAdapter implements VenueAdapter {
        private VenueAdapterCallback callback;
        @Override public RingWriter childOrderRingWriter(final int venueId) { return childOrder -> true; }
        @Override public void callback(final VenueAdapterCallback callback) { this.callback = callback; }
        void fill() { callback.deliverFill(new FillReport().set(1, 1, 1, 10, 100, 1)); }
        void reject() { callback.deliverReject(new RejectReport().set(1, 1, 1, 7, 1)); }
    }

    static final class FakePersistence implements Persistence {
        private final List<LifecycleEvent> events = new CopyOnWriteArrayList<>();
        @Override public long appendLifecycleEvent(final LifecycleEvent event) { event.sequenceNumber(events.size() + 1L); events.add(event); return event.sequenceNumber(); }
        @Override public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {}
        @Override public byte[] readPolicySnapshot(final long policyVersion) { return new byte[0]; }
        @Override public long lastPersistedPolicyVersion() { return 0; }
        @Override public Iterator<LifecycleEvent> replay(final long sinceSequence) { return events.iterator(); }
    }
}
