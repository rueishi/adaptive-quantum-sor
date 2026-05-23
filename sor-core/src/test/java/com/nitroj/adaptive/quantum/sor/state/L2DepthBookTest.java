package com.nitroj.adaptive.quantum.sor.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link L2DepthBook}.
 *
 * <p>Role in system: verifies the optional L2 shell stores depth quantity and
 * rejects invalid levels or values.</p>
 *
 * <p>Relationships: independent from top-of-book state.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: even shell structures should enforce dimensional bounds.</p>
 */
final class L2DepthBookTest {
    @Test
    void updateLevelAndBidQtyWork() {
        final L2DepthBook depth = new L2DepthBook(1, 1, 2);
        depth.updateLevel(0, 0, 1, 99, 10, 102, 20);

        assertEquals(99, depth.bidPriceTicks(0, 0, 1));
        assertEquals(10, depth.bidQty(0, 0, 1));
        assertEquals(102, depth.askPriceTicks(0, 0, 1));
        assertEquals(20, depth.askQty(0, 0, 1));
    }

    @Test
    void invalidDepthInputsFail() {
        final L2DepthBook depth = new L2DepthBook(1, 1, 2);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new L2DepthBook(1, 1, 0)).getMessage().contains("depthLevels"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> depth.bidQty(0, 0, 2)).getMessage().contains("level"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> depth.updateLevel(0, 0, 0, 0, 10, 102, 20)).getMessage().contains("prices"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> depth.updateLevel(0, 0, 0, 102, 10, 102, 20)).getMessage().contains("bid must be less than ask"));
    }
}
