package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link ChildOrder}.
 *
 * <p>Role in system: verifies mutable child-order slots are populated and
 * cleared correctly for allocation-free buffer reuse.</p>
 *
 * <p>Relationships: {@link ChildOrderBufferTest} verifies buffer ownership of
 * these slots.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: direct field assertions are intentional for primitive
 * hot-path records.</p>
 */
final class ChildOrderTest {
    @Test
    void setPopulatesAndClearResetsFields() {
        final ChildOrder order = new ChildOrder();

        order.set(10, 1, 2, 3, Side.SELL, 50, 7, 99);

        assertEquals(10, order.childOrderId);
        assertEquals(1, order.parentOrderId);
        assertEquals(2, order.instrumentId);
        assertEquals(3, order.venueId);
        assertEquals(Side.SELL, order.side);
        assertEquals(50, order.quantity);
        assertEquals(7, order.policyVersion);
        assertEquals(99, order.policyHash64);
        assertEquals(OrderStatus.NEW, order.status);

        order.clear();

        assertEquals(0, order.childOrderId);
        assertEquals(0, order.quantity);
        assertEquals(OrderStatus.NEW, order.status);
    }

    @Test
    void setRejectsInvalidFields() {
        final ChildOrder order = new ChildOrder();
        assertTrue(assertThrows(IllegalArgumentException.class, () -> order.set(0, 1, 0, 0, Side.BUY, 1, 1, 1)).getMessage().contains("order IDs"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> order.set(1, 1, -1, 0, Side.BUY, 1, 1, 1)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> order.set(1, 1, 0, 0, 99, 1, 1, 1)).getMessage().contains("side"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> order.set(1, 1, 0, 0, Side.BUY, 0, 1, 1)).getMessage().contains("quantity"));
    }
}
