package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualClockDeterminismTest {
    @Test
    void sameAdvancementProducesSameByteStream() {
        assertArrayEquals(stream(), stream());
    }

    @Test
    void clockDoesNotMoveUnlessAdvanced() {
        final ManualClock clock = new ManualClock(17);
        assertEquals(17, clock.nanoTime());
        assertEquals(17, clock.epochNanos());
        assertEquals(22, clock.advanceNanos(5));
    }

    private static byte[] stream() {
        final ManualClock clock = new ManualClock(100);
        final byte[] bytes = new byte[4];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) clock.advanceNanos(10);
        }
        return bytes;
    }
}
