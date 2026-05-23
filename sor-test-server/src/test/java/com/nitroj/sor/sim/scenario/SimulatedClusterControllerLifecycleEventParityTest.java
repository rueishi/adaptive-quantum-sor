package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies lifecycle failure evidence through the simulator cluster controller. */
final class SimulatedClusterControllerLifecycleEventParityTest {
    @Test
    void failedGuardCreatesReplayableLifecycleEvent() {
        final ManualClock clock = new ManualClock(7L);
        final SimulatedClusterController cluster = new SimulatedClusterController(clock,
                new SimulatedMarketDataSource(), new SimulatedVenueAdapter(clock));

        cluster.runGuarded("venue", () -> { throw new IllegalArgumentException("boom"); });

        assertFalse(cluster.publicationAllowed());
        assertTrue(cluster.persistence().replay(0).hasNext());
    }
}
