package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.state.FeedHealthState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify the stateful market-data simulator contract.
 *
 * <p>Role in system: this class is the deterministic simulator-test surface for
 * P5-TC-002, separate from scenario-driven orchestration tests.</p>
 *
 * <p>Relationships: exercises {@link MarketDataSimulator} writes into
 * {@link MarketBookState}, optional reads from {@link RegimeState}, and optional
 * feed-health publication to {@link FeedHealthState}.</p>
 *
 * <p>Lifecycle: run by Gradle/JUnit as a unit test whenever simulator behavior
 * changes.</p>
 *
 * <p>Design intent: prove the low-level simulator emits valid, replayable,
 * correlated L1 books without depending on the higher-level scenario runner.</p>
 */
final class MarketDataSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void generateTickPublishesValidBook() {
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, 31L);

        simulator.generateTick(book);

        assertEquals(CONFIG.instrumentCount() * CONFIG.venueCount(), book.sequence());
        assertEquals(1L, simulator.tick());
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(simulator.currentMidTicks(instrumentId) > 0);
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertTrue(book.bidPriceTicks(instrumentId, venueId) > 0);
                assertTrue(book.askPriceTicks(instrumentId, venueId) > book.bidPriceTicks(instrumentId, venueId));
                assertTrue(book.bidQty(instrumentId, venueId) >= 0);
                assertTrue(book.askQty(instrumentId, venueId) >= 0);
                assertEquals(book.bidQty(instrumentId, venueId), simulator.currentBidQty(instrumentId, venueId));
                assertEquals(book.askQty(instrumentId, venueId), simulator.currentAskQty(instrumentId, venueId));
            }
        }
    }

    @Test
    void sameSeedProducesSameSingleTick() {
        final MarketBookState firstBook = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketBookState secondBook = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator first = new MarketDataSimulator(CONFIG, 41L);
        final MarketDataSimulator second = new MarketDataSimulator(CONFIG, 41L);

        first.generateTick(firstBook);
        second.generateTick(secondBook);

        assertBooksEqual(firstBook, secondBook);
        assertEquals(first.tick(), second.tick());
        assertEquals(first.currentMidTicks(0), second.currentMidTicks(0));
        assertEquals(first.currentMidTicks(1), second.currentMidTicks(1));
    }

    @Test
    void applySanitizedRejectsOrCorrectsInvalidRawValues() {
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, 51L);

        simulator.applySanitized(book, 0, 0, -100L, -90L, -10L, -20L);
        assertEquals(1L, book.bidPriceTicks(0, 0));
        assertEquals(2L, book.askPriceTicks(0, 0));
        assertEquals(0L, book.bidQty(0, 0));
        assertEquals(0L, book.askQty(0, 0));

        simulator.applySanitized(book, 0, 1, 100L, 100L, 10L, 20L);
        assertEquals(100L, book.bidPriceTicks(0, 1));
        assertEquals(101L, book.askPriceTicks(0, 1));
        assertEquals(10L, book.bidQty(0, 1));
        assertEquals(20L, book.askQty(0, 1));
    }

    @Test
    void regimeOverloadChangesStatefulSpreadAndDepth() {
        final MarketBookState normalBook = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketBookState thinBook = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator normal = new MarketDataSimulator(CONFIG, 61L);
        final MarketDataSimulator thin = new MarketDataSimulator(CONFIG, 61L);
        final RegimeState regimes = new RegimeState(CONFIG.instrumentCount());
        regimes.setRegime(0, RegimeState.THIN_BOOK);
        regimes.setRegime(1, RegimeState.THIN_BOOK);

        normal.generateTick(normalBook);
        thin.generateTick(thinBook, regimes);

        assertTrue(totalQty(thinBook) < totalQty(normalBook));
        assertTrue(maxSpread(thinBook) >= maxSpread(normalBook));
    }

    @Test
    void scenarioOverloadPublishesFeedHealthDeterministically() {
        final ScenarioSpec spec = ScenarioSpec.defaultSpec(CONFIG, 71L);
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final FeedHealthState feedHealth = new FeedHealthState(CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, spec.seed());

        simulator.generateTick(book, scenario, feedHealth);

        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            assertEquals(1L, feedHealth.lastSequence(venueId));
        }
    }

    @Test
    void publicViewsRejectOutOfRangeIds() {
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, 81L);

        assertThrows(IndexOutOfBoundsException.class, () -> simulator.currentMidTicks(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> simulator.currentMidTicks(CONFIG.instrumentCount()));
        assertThrows(IndexOutOfBoundsException.class, () -> simulator.currentBidQty(0, CONFIG.venueCount()));
        assertThrows(IndexOutOfBoundsException.class, () -> simulator.currentAskQty(CONFIG.instrumentCount(), 0));
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.generateTick(null)
        ).getMessage());
    }

    private static void assertBooksEqual(final MarketBookState first, final MarketBookState second) {
        assertEquals(first.sequence(), second.sequence());
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertEquals(first.bidPriceTicks(instrumentId, venueId), second.bidPriceTicks(instrumentId, venueId));
                assertEquals(first.askPriceTicks(instrumentId, venueId), second.askPriceTicks(instrumentId, venueId));
                assertEquals(first.bidQty(instrumentId, venueId), second.bidQty(instrumentId, venueId));
                assertEquals(first.askQty(instrumentId, venueId), second.askQty(instrumentId, venueId));
            }
        }
    }

    private static long totalQty(final MarketBookState book) {
        long total = 0L;
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                total += book.bidQty(instrumentId, venueId) + book.askQty(instrumentId, venueId);
            }
        }
        return total;
    }

    private static long maxSpread(final MarketBookState book) {
        long max = 0L;
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                max = Math.max(max, book.askPriceTicks(instrumentId, venueId) - book.bidPriceTicks(instrumentId, venueId));
            }
        }
        return max;
    }
}
