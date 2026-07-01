package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies public reset/control-plane DTO contracts.
 *
 * <p>Role in system: protects the Phase 9 typed reset API from ambiguous or
 * mutable reset evidence.</p>
 *
 * <p>Relationships: covers {@link SorResetMode}, {@link SorResetRequest},
 * {@link SorResetSummary}, and {@link SorControlPlane} DTO expectations without
 * depending on `sor-core`.</p>
 *
 * <p>Lifecycle: unit test run by `:sor-api:test` for every public API change.</p>
 *
 * <p>Design intent: keep destructive reset intent explicit and reset summaries
 * immutable.</p>
 */
class SorResetApiTest {
    /**
     * Confirms reset requests validate required fields and preserve safety
     * flags.
     */
    @Test
    void resetRequestValidatesInputs() {
        final SorResetRequest request = new SorResetRequest(SorResetMode.CLEAR_MARKET_DATA, true, "scenario reset");

        assertEquals(SorResetMode.CLEAR_MARKET_DATA, request.mode());
        assertEquals(true, request.requireNoLiveOrders());
        assertEquals("scenario reset", request.reason());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorResetRequest(null, true, "x")).getMessage().contains("mode"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorResetRequest(SorResetMode.APPEND, true, " ")).getMessage().contains("reason"));
    }

    /**
     * Confirms reset summaries defensively copy array evidence.
     */
    @Test
    void resetSummaryDefensivelyCopiesArrays() {
        final String[] cleared = {"marketBook"};
        final SorResetSummary summary = new SorResetSummary(SorResetMode.CLEAR_MARKET_DATA, true, true,
                "ok", cleared, new String[]{"activePolicy"}, new String[]{});

        cleared[0] = "mutated";
        final String[] read = summary.clearedState();
        read[0] = "mutated-again";

        assertArrayEquals(new String[]{"marketBook"}, summary.clearedState());
    }

    /**
     * Confirms destructive reset modes are distinguishable from append mode.
     */
    @Test
    void resetModesExposeDestructiveFlag() {
        assertEquals(true, SorResetMode.CLEAR_MARKET_DATA.destructive());
        assertEquals(true, SorResetMode.SCENARIO_REPLAY_RESET.destructive());
        assertEquals(false, SorResetMode.APPEND.destructive());
    }
}
