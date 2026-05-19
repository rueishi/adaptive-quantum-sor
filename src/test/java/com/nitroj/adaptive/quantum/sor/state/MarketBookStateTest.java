package com.nitroj.adaptive.quantum.sor.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link MarketBookState}.
 *
 * <p>Role in system: verifies top-of-book update, accessors, sequencing, and
 * invalid market-data rejection.</p>
 *
 * <p>Relationships: isolated state test using the canonical IV indexing
 * indirectly through public methods.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: invalid crossed/negative books must fail before later
 * routing logic reads them.</p>
 */
final class MarketBookStateTest {
    @Test
    void updateAndAccessorsWork() {
        final MarketBookState book = new MarketBookState(1, 1);
        book.updateTopOfBook(0, 0, 100, 101, 1_000, 2_000);

        assertEquals(100, book.bidPriceTicks(0, 0));
        assertEquals(101, book.askPriceTicks(0, 0));
        assertEquals(1_000, book.bidQty(0, 0));
        assertEquals(2_000, book.askQty(0, 0));
        assertEquals(1, book.sequence());
    }

    @Test
    void invalidUpdatesFailClearly() {
        final MarketBookState book = new MarketBookState(1, 1);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> book.updateTopOfBook(0, 0, 101, 101, 1, 1)).getMessage().contains("bid must be less than ask"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> book.updateTopOfBook(0, 0, 100, 101, -1, 1)).getMessage().contains("quantities"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> book.updateTopOfBook(0, 0, 0, 101, 1, 1)).getMessage().contains("prices"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MarketBookState(0, 1)).getMessage().contains("instrumentCount"));
    }
}
