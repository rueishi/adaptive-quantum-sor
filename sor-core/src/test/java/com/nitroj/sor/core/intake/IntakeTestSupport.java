package com.nitroj.sor.core.intake;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.VenueAdapter;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Verifies intake test support behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
final class IntakeTestSupport {
    private IntakeTestSupport() {}

    static SorEngineBuilder builder(final int capacity) {
        return SorEngineBuilder.create()
                .config(new SorConfig(capacity, 100))
                .marketData(new MarketDataSource() {
                    @Override public void subscribe(final int instrumentId, final MarketDataListener listener) {}
                    @Override public void unsubscribe(final int instrumentId, final MarketDataListener listener) {}
                })
                .venueAdapter(new VenueAdapter() {
                    @Override public RingWriter childOrderRingWriter(final int venueId) { return childOrder -> true; }
                    @Override public void callback(final VenueAdapterCallback callback) {}
                })
                .riskProvider((request, decision) -> decision.allow())
                .persistence(new Persistence() {
                    @Override public long appendLifecycleEvent(final LifecycleEvent event) { return 0; }
                    @Override public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {}
                    @Override public byte[] readPolicySnapshot(final long policyVersion) { return new byte[0]; }
                    @Override public long lastPersistedPolicyVersion() { return 0; }
                    @Override public Iterator<LifecycleEvent> replay(final long sinceSequence) { return java.util.List.<LifecycleEvent>of().iterator(); }
                })
                .clock(new Clock() {
                    private final AtomicLong now = new AtomicLong(1);
                    @Override public long nanoTime() { return now.incrementAndGet(); }
                    @Override public long epochNanos() { return now.incrementAndGet(); }
                });
    }

    static ParentOrderRequest request(final long quantity) {
        return ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(quantity).urgency(0).build();
    }
}
