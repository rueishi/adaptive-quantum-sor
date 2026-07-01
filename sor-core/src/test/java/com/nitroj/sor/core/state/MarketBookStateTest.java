package com.nitroj.sor.core.state;

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
    void clearRemovesTopOfBookAndResetsSequence() {
        final MarketBookState book = new MarketBookState(1, 1);
        book.updateTopOfBook(0, 0, 100, 101, 1_000, 2_000, 123);

        book.clear();

        assertEquals(0, book.bidPriceTicks(0, 0));
        assertEquals(0, book.askPriceTicks(0, 0));
        assertEquals(0, book.bidQty(0, 0));
        assertEquals(0, book.askQty(0, 0));
        assertEquals(0, book.sequence());
        assertEquals(0, book.populatedCellCount());
        assertEquals(0, book.lastUpdateEpochNanos());
    }

    @Test
    void diagnosticFieldsTrackPopulationTimestampAndChecksum() {
        final MarketBookState book = new MarketBookState(1, 2);

        book.updateTopOfBook(0, 0, 100, 101, 1_000, 2_000, 123);
        final long firstChecksum = book.checksum();
        book.updateTopOfBook(0, 1, 200, 201, 3_000, 4_000, 122);
        final long secondChecksum = book.checksum();

        assertEquals(2, book.sequence());
        assertEquals(2, book.populatedCellCount());
        assertEquals(123, book.lastUpdateEpochNanos());
        assertTrue(firstChecksum != secondChecksum);
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
