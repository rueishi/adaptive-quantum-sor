package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-23 simulator supervisor and publication-guard parity. */
final class LegacyPublicationGuardParityTest {
    @Test
    void guardedFailureMarksHealthAndBlocksPublication() {
        final ManualClock clock = new ManualClock(42L);
        final SimulatedClusterController cluster = new SimulatedClusterController(
                clock, new SimulatedMarketDataSource(), new SimulatedVenueAdapter(clock));

        assertFalse(cluster.runGuarded("marketData", () -> { throw new IllegalStateException("bad tick"); }));

        assertFalse(cluster.publicationAllowed());
        assertEquals(1, cluster.healthState().failureCount());
        assertEquals("marketData", cluster.healthState().failedSimulatorName());
        assertEquals("bad tick", cluster.healthState().failureMessage());
        assertTrue(cluster.persistence().replay(0).hasNext());
    }
}
