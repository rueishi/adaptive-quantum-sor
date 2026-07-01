package com.nitroj.sor.testkit.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic scenario summary contracts.
 *
 * <p>Role in system: scenario replay tests compare public summaries instead of
 * random internals.</p>
 *
 * <p>Relationships: tests {@link ScenarioSummary} and {@link ScenarioAssertions}.</p>
 *
 * <p>Lifecycle: executed as P5-TC-001 unit coverage.</p>
 *
 * <p>Design intent: summary equality must be stable and assertion-friendly.</p>
 */
final class ScenarioSummaryTest {
    @Test
    void sameInputsProduceEqualSummaries() {
        final ScenarioSummary first = summary("s", 7L, 3, 11L);
        final ScenarioSummary second = summary("s", 7L, 3, 11L);

        assertEquals(first, second);
        ScenarioAssertions.assertReplayEquivalent(first, second);
        assertEquals(11L, first.bookChecksum());
    }

    @Test
    void scenarioSummaryAndAssertionsRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> summary("", 1L, 1, 1L));
        assertThrows(IllegalArgumentException.class, () -> summary("s", 1L, -1, 1L));
        assertThrows(IllegalArgumentException.class, () -> ScenarioAssertions.assertReplayEquivalent(null, summary("s", 1L, 1, 1L)));
        assertThrows(AssertionError.class, () -> ScenarioAssertions.assertReplayEquivalent(
                summary("s", 1L, 1, 1L),
                summary("s", 1L, 1, 2L)
        ));
    }

    @Test
    void summaryFieldsAreStableAndPrimitiveOrStringBacked() {
        final ScenarioSummary summary = summary("primitive", 3L, 4, 5L);

        assertEquals(String.class, summary.scenarioId().getClass());
        assertEquals(3L, summary.seed());
        assertEquals(4, summary.ticksRun());
        assertEquals(5L, summary.bookChecksum());
        assertEquals(1, summary.orderCount());
        assertEquals(2, summary.childOrderCount());
        assertEquals(3, summary.outcomeCount());
        assertEquals(8L, summary.residualQty());
    }

    private static ScenarioSummary summary(final String id, final long seed, final int ticks, final long checksum) {
        return new ScenarioSummary(id, seed, ticks, checksum, 1, 2, 3, 4, 5, 6, 7, 8L,
                9L, 10L, 11L, 12L, 13, 14, 15, 16, 17);
    }
}
