package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.RejectReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verifies the engine-owned working order book.
 *
 * <p>Role in system: proves Phase 9 parent and child order state can be
 * maintained locally without depending on OMS/EMS reads during routing.</p>
 *
 * <p>Relationships: covers {@link EngineOrderBook} snapshots used by
 * `SorEngineImpl` and future reset/diagnostic control-plane cards.</p>
 *
 * <p>Lifecycle: unit-level test that creates a fresh order book per case.</p>
 *
 * <p>Design intent: keep order-state accounting deterministic and explicit
 * before venue emission and scenario replay are wired through later cards.</p>
 */
class EngineOrderBookTest {
    /**
     * Confirms accepting a parent creates local working state.
     */
    @Test
    void acceptParentCreatesWorkingState() {
        final EngineOrderBook book = new EngineOrderBook();

        final EngineOrderBook.ParentSnapshot snapshot = book.acceptParent(1, 100, 1_000);

        assertEquals(1, snapshot.parentOrderId());
        assertEquals(100, snapshot.originalQuantity());
        assertEquals(100, snapshot.remainingQuantity());
        assertEquals(OrderStatusCode.ACCEPTED, snapshot.status());
    }

    /**
     * Confirms emitted child orders are tracked as pending quantity by parent
     * and venue.
     */
    @Test
    void recordChildOrderTracksPendingQuantity() {
        final EngineOrderBook book = new EngineOrderBook();
        book.acceptParent(1, 100, 1_000);

        final EngineOrderBook.ParentSnapshot parent = book.recordChildOrder(10, 1, 2, 40, 1_100);
        final EngineOrderBook.ChildSnapshot child = book.child(10);

        assertEquals(40, parent.pendingChildQuantity());
        assertEquals(OrderStatusCode.ROUTED, parent.status());
        assertEquals(2, child.venueId());
        assertEquals(40, child.remainingQuantity());
    }

    /**
     * Confirms fills update child state, parent fill totals, and remaining
     * parent quantity.
     */
    @Test
    void fillUpdatesParentAndChildState() {
        final EngineOrderBook book = new EngineOrderBook();
        book.acceptParent(1, 100, 1_000);
        book.recordChildOrder(10, 1, 2, 40, 1_100);

        final EngineOrderBook.ParentSnapshot parent = book.applyFill(
                new FillReport().set(10, 1, 2, 25, 101, 1_200));
        final EngineOrderBook.ChildSnapshot child = book.child(10);

        assertEquals(25, parent.filledQuantity());
        assertEquals(75, parent.remainingQuantity());
        assertEquals(15, parent.pendingChildQuantity());
        assertEquals(OrderStatusCode.PARTIALLY_FILLED, parent.status());
        assertEquals(15, child.remainingQuantity());
        assertEquals(25, child.filledQuantity());
    }

    /**
     * Confirms rejects clear pending child quantity and mark parent state
     * rejected.
     */
    @Test
    void rejectUpdatesParentAndChildState() {
        final EngineOrderBook book = new EngineOrderBook();
        book.acceptParent(1, 100, 1_000);
        book.recordChildOrder(10, 1, 2, 40, 1_100);

        final EngineOrderBook.ParentSnapshot parent = book.applyReject(
                new RejectReport().set(10, 1, 2, 7, 1_200));
        final EngineOrderBook.ChildSnapshot child = book.child(10);

        assertEquals(0, parent.pendingChildQuantity());
        assertEquals(OrderStatusCode.REJECTED, parent.status());
        assertEquals(0, child.remainingQuantity());
        assertEquals(OrderStatusCode.REJECTED, child.status());
    }

    /**
     * Confirms recovery/scenario state can be seeded without passing through
     * new-order submission.
     */
    @Test
    void seedParentRestoresSnapshotWithoutSubmit() {
        final EngineOrderBook book = new EngineOrderBook();
        final EngineOrderBook.ParentSnapshot seed = new EngineOrderBook.ParentSnapshot(
                9, 100, 30, 70, 20, OrderStatusCode.PARTIALLY_FILLED, 2_000);

        book.seedParent(seed);

        assertEquals(seed, book.parent(9));
    }

    /**
     * Confirms invalid child emissions fail clearly instead of silently
     * creating orphan child state.
     */
    @Test
    void recordChildOrderRejectsUnknownParent() {
        final EngineOrderBook book = new EngineOrderBook();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> book.recordChildOrder(10, 99, 2, 40, 1_100));

        assertEquals("unknown parentOrderId: 99", exception.getMessage());
    }
}
