package com.nitroj.adaptive.quantum.sor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link OrderIntent}.
 *
 * <p>Role in system: verifies parent-order construction and validation before
 * intents enter engine queues.</p>
 *
 * <p>Relationships: isolated from queue behavior, which is tested in
 * {@link ParentOrderIntentQueueTest}.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: public fields are asserted directly because the class is a
 * primitive hot-path carrier.</p>
 */
final class OrderIntentTest {
    @Test
    void constructorStoresFields() {
        final OrderIntent intent = new OrderIntent(1, 2, Side.BUY, 100, 3, 4);

        assertEquals(1, intent.parentOrderId);
        assertEquals(2, intent.instrumentId);
        assertEquals(Side.BUY, intent.side);
        assertEquals(100, intent.quantity);
        assertEquals(3, intent.urgencyId);
        assertEquals(4, intent.createdAtNanos);
        assertEquals(Side.BUY, Side.parse("BUY"));
        assertEquals(Side.SELL, Side.parse("SELL"));
        assertEquals(Side.BUY, Side.parse("1"));
        assertEquals(Side.SELL, Side.parse("2"));
    }

    @Test
    void constructorRejectsInvalidFields() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderIntent(0, 0, Side.BUY, 1, 0, 0)).getMessage().contains("parentOrderId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderIntent(1, -1, Side.BUY, 1, 0, 0)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderIntent(1, 0, 99, 1, 0, 0)).getMessage().contains("side"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderIntent(1, 0, Side.BUY, 0, 0, 0)).getMessage().contains("quantity"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderIntent(1, 0, Side.BUY, 1, -1, 0)).getMessage().contains("urgencyId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Side.parse("SHORT")).getMessage().contains("side"));
    }
}
