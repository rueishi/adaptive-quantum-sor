package com.nitroj.adaptive.quantum.sor.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link L3OrderBook}.
 *
 * <p>Role in system: verifies the optional L3 shell records event sequence and
 * count while rejecting backwards sequence movement.</p>
 *
 * <p>Relationships: isolated shell-state test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: sequence monotonicity is a core assumption for future queue
 * modeling.</p>
 */
final class L3OrderBookTest {
    @Test
    void recordEventUpdatesSequenceAndCount() {
        final L3OrderBook l3 = new L3OrderBook();
        l3.recordEvent(5);

        assertEquals(5, l3.sequence());
        assertEquals(1, l3.eventCount());
    }

    @Test
    void backwardsSequenceFails() {
        final L3OrderBook l3 = new L3OrderBook();
        l3.recordEvent(5);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> l3.recordEvent(4)).getMessage().contains("sequence"));
    }
}
