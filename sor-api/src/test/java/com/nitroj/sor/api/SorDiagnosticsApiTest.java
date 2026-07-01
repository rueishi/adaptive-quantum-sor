package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies public diagnostic DTO contracts.
 *
 * <p>Role in system: protects safe control-plane diagnostics from mutable
 * engine internals or invalid summary values.</p>
 *
 * <p>Relationships: covers {@link SorStateSummary} and
 * {@link MarketDataSnapshotSummary} independently from `sor-core`.</p>
 *
 * <p>Lifecycle: unit test run by `:sor-api:test` for every public API change.</p>
 *
 * <p>Design intent: keep notebook/test diagnostics typed, compact, and
 * impossible to confuse with live state handles.</p>
 */
class SorDiagnosticsApiTest {
    @Test
    void stateSummaryAllowsMissingLastResetAndValidatesCounts() {
        final SorStateSummary summary = new SorStateSummary(1, 2, 3, 4, null);

        assertEquals(1, summary.activeParentOrderCount());
        assertEquals(2, summary.activeChildOrderCount());
        assertEquals(3, summary.pendingChildQuantity());
        assertEquals(4, summary.activePolicyVersion());
        assertNull(summary.lastResetSummary());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorStateSummary(-1, 0, 0, 0, null)).getMessage().contains("non-negative"));
    }

    @Test
    void marketDataSnapshotSummaryValidatesNonNegativeFields() {
        final MarketDataSnapshotSummary summary = new MarketDataSnapshotSummary(1, -7, 2, 3);

        assertEquals(1, summary.sequence());
        assertEquals(-7, summary.checksum());
        assertEquals(2, summary.populatedCellCount());
        assertEquals(3, summary.lastUpdateEpochNanos());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MarketDataSnapshotSummary(-1, 0, 0, 0)).getMessage().contains("non-negative"));
    }
}
