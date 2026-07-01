package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies child-order startup snapshot coverage.
 *
 * <p>Role in system: ensures P9 startup hydration cannot regress to
 * parent-only recovery, which would lose venue-resting children and parent
 * pending quantity.</p>
 *
 * <p>Relationships: covers {@link ChildOrderStateSnapshot} fields required by
 * future engine bulk apply and self-liquidity checks.</p>
 *
 * <p>Lifecycle: run by `:sor-api:test` before engine hydration work starts.</p>
 *
 * <p>Design intent: make child snapshot completeness a public API contract.</p>
 */
class OrderStateSnapshotChildContractTest {
    @Test
    void childSnapshotCarriesVenueQuantityPriceStatusAndParentLinkage() {
        final ChildOrderStateSnapshot child = new ChildOrderStateSnapshot(
                101, 7, 3, 5, Side.BUY, 12_345, 1_000, 750, 250,
                OrderStatusCode.PARTIALLY_FILLED, 999);

        assertEquals(101, child.childOrderId());
        assertEquals(7, child.parentOrderId());
        assertEquals(3, child.instrumentId());
        assertEquals(5, child.venueId());
        assertEquals(Side.BUY, child.side());
        assertEquals(12_345, child.limitPrice());
        assertEquals(1_000, child.originalQuantity());
        assertEquals(750, child.leavesQuantity());
        assertEquals(250, child.filledQuantity());
        assertEquals(OrderStatusCode.PARTIALLY_FILLED, child.status());
        assertEquals(999, child.updatedEpochNanos());
    }

    @Test
    void childSnapshotRejectsInvalidVenueQuantitySideAndStatus() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ChildOrderStateSnapshot(1, 1, 1, -1, Side.BUY, 0,
                        1, 1, 0, OrderStatusCode.ROUTED, 1)).getMessage().contains("child"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ChildOrderStateSnapshot(1, 1, 1, 1, 99, 0,
                        1, 1, 0, OrderStatusCode.ROUTED, 1)).getMessage().contains("child"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ChildOrderStateSnapshot(1, 1, 1, 1, Side.BUY, 0,
                        10, 8, 8, OrderStatusCode.ROUTED, 1)).getMessage().contains("filled plus leaves"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new ChildOrderStateSnapshot(1, 1, 1, 1, Side.BUY, 0,
                        1, 1, 0, null, 1)).getMessage().contains("child"));
    }
}
