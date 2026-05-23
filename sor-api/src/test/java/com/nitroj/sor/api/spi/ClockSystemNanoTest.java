package com.nitroj.sor.api.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the default system clock implementation.
 *
 * <p>Role in system: P8-04 requires a universal injectable clock source.</p>
 *
 * <p>Relationships: covers {@link Clock#systemNano()} and its package-private
 * implementation.</p>
 *
 * <p>Lifecycle: creates the singleton clock and samples it in a tight loop.</p>
 *
 * <p>Design intent: keep direct `System` time calls isolated and auditable.</p>
 */
class ClockSystemNanoTest {
    /**
     * Confirms monotonic nano time and approximate epoch nanos.
     */
    @Test
    void systemNanoClockIsMonotonicAndEpochBased() {
        final Clock clock = Clock.systemNano();
        long previous = clock.nanoTime();
        for (int i = 0; i < 1000; i++) {
            final long current = clock.nanoTime();
            assertTrue(current >= previous);
            previous = current;
        }

        final long nowMillis = System.currentTimeMillis();
        final long epochNanos = clock.epochNanos();
        assertTrue(Math.abs(epochNanos - nowMillis * 1_000_000L) < 1_000_000_000L);
    }
}
