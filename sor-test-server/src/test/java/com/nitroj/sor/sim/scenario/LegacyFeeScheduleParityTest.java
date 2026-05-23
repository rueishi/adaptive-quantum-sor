package com.nitroj.sor.sim.scenario;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.venues.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies P8-23 fee schedule parity with the legacy deterministic formula. */
final class LegacyFeeScheduleParityTest {
    @Test
    void generatePreservesMakerTakerLayout() {
        final SimConfig config = new SimConfig(2, 6, 3, 2);
        final SimFeeScheduleSnapshot snapshot = new SimulatedFeeSchedule().generate(config);

        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                assertEquals(venueId % 3, snapshot.makerBps(instrumentId, venueId));
                assertEquals(1 + venueId % 5, snapshot.takerBps(instrumentId, venueId));
            }
        }
    }

    @Test
    void rejectsInvalidConfig() {
        assertEquals("config must not be null",
                assertThrows(IllegalArgumentException.class, () -> new SimulatedFeeSchedule().generate(null)).getMessage());
    }
}
