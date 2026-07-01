package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies startup order and market snapshot contracts.
 *
 * <p>Role in system: protects P9-TC-018 snapshot validation before the engine
 * consumes these records during warm-path hydration.</p>
 *
 * <p>Relationships: covers {@link OrderStateSnapshot},
 * {@link ParentOrderStateSnapshot}, {@link ChildOrderStateSnapshot},
 * {@link MarketDataSeedSnapshot}, and {@link MarketDataSeedCell}.</p>
 *
 * <p>Lifecycle: run by `:sor-api:test` for every public snapshot contract
 * change.</p>
 *
 * <p>Design intent: distinguish valid empty snapshots, valid child-bearing
 * snapshots, corrupt duplicate IDs, and duplicate market cells.</p>
 */
class StartupSnapshotContractTest {
    @Test
    void emptyOrderSnapshotIsAuditable() {
        final OrderStateSnapshot empty = OrderStateSnapshot.empty("empty-orders", 44, 55);

        assertEquals("empty-orders", empty.snapshotId());
        assertEquals(44, empty.asOfSequence());
        assertEquals(55, empty.asOfEpochNanos());
        assertEquals(0, empty.parents().length);
        assertEquals(0, empty.children().length);
    }

    @Test
    void orderSnapshotDefensivelyCopiesArrays() {
        final ParentOrderStateSnapshot[] parents = {parent(1)};
        final ChildOrderStateSnapshot[] children = {child(11, 1)};
        final OrderStateSnapshot snapshot = new OrderStateSnapshot("orders", 1, 2, parents, children);

        parents[0] = parent(2);
        children[0] = child(12, 2);
        final ParentOrderStateSnapshot[] readParents = snapshot.parents();
        readParents[0] = parent(3);

        assertArrayEquals(new ParentOrderStateSnapshot[]{parent(1)}, snapshot.parents());
        assertArrayEquals(new ChildOrderStateSnapshot[]{child(11, 1)}, snapshot.children());
    }

    @Test
    void orderSnapshotRejectsDuplicateIdsAndMissingParentLinkage() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new OrderStateSnapshot("orders", 1, 2,
                        new ParentOrderStateSnapshot[]{parent(1), parent(1)},
                        new ChildOrderStateSnapshot[0])).getMessage().contains("duplicate parentOrderId"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new OrderStateSnapshot("orders", 1, 2,
                        new ParentOrderStateSnapshot[]{parent(1)},
                        new ChildOrderStateSnapshot[]{child(11, 1), child(11, 1)}))
                .getMessage().contains("duplicate childOrderId"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new OrderStateSnapshot("orders", 1, 2,
                        new ParentOrderStateSnapshot[]{parent(1)},
                        new ChildOrderStateSnapshot[]{child(11, 99)}))
                .getMessage().contains("missing parentOrderId"));
    }

    @Test
    void marketSnapshotValidatesCellsAndDefensivelyCopiesArray() {
        final MarketDataSeedCell[] cells = {new MarketDataSeedCell(1, 2, 100, 101, 10, 11, 12)};
        final MarketDataSeedSnapshot snapshot = new MarketDataSeedSnapshot("market", 3, 4, cells);

        cells[0] = new MarketDataSeedCell(2, 3, 100, 101, 10, 11, 12);
        assertEquals(1, snapshot.cells()[0].instrumentId());
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MarketDataSeedCell(1, 2, 101, 100, 10, 11, 12)).getMessage().contains("bid"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MarketDataSeedSnapshot("market", 3, 4,
                        new MarketDataSeedCell[]{
                                new MarketDataSeedCell(1, 2, 100, 101, 10, 11, 12),
                                new MarketDataSeedCell(1, 2, 102, 103, 10, 11, 13)
                        })).getMessage().contains("duplicate"));
    }

    private static ParentOrderStateSnapshot parent(final long parentOrderId) {
        return new ParentOrderStateSnapshot(parentOrderId, 1, Side.SELL, 99, 100, 70,
                30, 40, OrderStatusCode.ROUTED, 10);
    }

    private static ChildOrderStateSnapshot child(final long childOrderId, final long parentOrderId) {
        return new ChildOrderStateSnapshot(childOrderId, parentOrderId, 1, 2, Side.SELL,
                99, 50, 40, 10, OrderStatusCode.ROUTED, 11);
    }
}
