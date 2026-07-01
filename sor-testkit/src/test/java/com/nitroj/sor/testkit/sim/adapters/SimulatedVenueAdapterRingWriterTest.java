package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.RejectReport;
import com.nitroj.sor.api.spi.VenueAdapter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies simulated venue adapter ring writer behavior for simulator SPI adapters.
 *
 * <p>Run with :sor-testkit:test to protect deterministic adapter behavior used by scenarios and local servers.</p>
 */
class SimulatedVenueAdapterRingWriterTest {
    @Test
    void offerOnlyWritesAndSimulatorThreadConsumes() throws Exception {
        final ManualClock clock = new ManualClock(10);
        final SimulatedVenueAdapter adapter = new SimulatedVenueAdapter(clock);
        final AtomicReference<FillReport> fill = new AtomicReference<>();
        adapter.callback(new VenueAdapter.VenueAdapterCallback() {
            @Override public void deliverFill(final FillReport report) { fill.set(report); }
            @Override public void deliverReject(final RejectReport report) {}
        });

        final boolean offered = adapter.childOrderRingWriter(2)
                .offer(new ChildOrderRef().set(99, 42, 2, Side.BUY, 100, 101, 10));

        final Thread consumer = new Thread(() -> adapter.pollVenue(2, 1), "sor-sim-venue-2");
        consumer.start();
        consumer.join();

        assertTrue(offered);
        assertEquals("sor-sim-venue-2", adapter.lastConsumerThreadName());
        assertEquals(42, fill.get().parentOrderId());
        assertEquals(100, fill.get().filledQuantity());
    }
}
