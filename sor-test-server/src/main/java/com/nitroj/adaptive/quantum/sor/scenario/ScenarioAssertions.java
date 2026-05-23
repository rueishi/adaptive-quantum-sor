package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.state.MarketBookState;

/**
 * Responsibility: provide deterministic scenario assertion helpers without
 * depending on a specific test framework.
 *
 * <p>Role in system: scenario tests and future notebook/demo validation can use
 * these helpers to compare public scenario results and published book state.</p>
 *
 * <p>Relationships: checks {@link ScenarioSummary} values and
 * {@link MarketBookState} invariants.</p>
 *
 * <p>Lifecycle: stateless utility used from tests or validation code.</p>
 *
 * <p>Design intent: keep replay assertions public-state oriented and avoid
 * comparing private random-generator internals.</p>
 */
public final class ScenarioAssertions {
    private ScenarioAssertions() {
    }

    public static void assertReplayEquivalent(final ScenarioSummary expected, final ScenarioSummary actual) {
        if (expected == null || actual == null) {
            throw new IllegalArgumentException("summaries must not be null");
        }
        if (!expected.equals(actual)) {
            throw new AssertionError("scenario summaries differ: expected=" + expected + ", actual=" + actual);
        }
    }

    public static void assertNoInvalidBooks(
            final MarketBookState state,
            final int instrumentCount,
            final int venueCount
    ) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int instrumentId = 0; instrumentId < instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < venueCount; venueId++) {
                if (state.bidPriceTicks(instrumentId, venueId) <= 0
                        || state.askPriceTicks(instrumentId, venueId) <= state.bidPriceTicks(instrumentId, venueId)
                        || state.bidQty(instrumentId, venueId) < 0
                        || state.askQty(instrumentId, venueId) < 0) {
                    throw new AssertionError("invalid book at instrument=" + instrumentId + ", venue=" + venueId);
                }
            }
        }
    }
}
