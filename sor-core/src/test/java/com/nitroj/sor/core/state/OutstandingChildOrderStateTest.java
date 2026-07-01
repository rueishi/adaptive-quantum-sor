package com.nitroj.sor.core.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link OutstandingChildOrderState}.
 *
 * <p>Role in system: verifies remaining-quantity tracking for outstanding
 * child orders.</p>
 *
 * <p>Relationships: complements {@link ChildOrderStateTest}, which covers
 * lifecycle status.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: overfills clamp at zero so later venue simulation cannot
 * create negative remaining quantity.</p>
 */
final class OutstandingChildOrderStateTest {
    @Test
    void putApplyFillAndAccessorsWork() {
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(1);
        outstanding.put(0, 100, 40);

        assertEquals(100, outstanding.childOrderId(0));
        assertEquals(40, outstanding.remainingQty(0));
        outstanding.applyFill(0, 15);
        assertEquals(25, outstanding.remainingQty(0));
        outstanding.applyFill(0, 100);
        assertEquals(0, outstanding.remainingQty(0));
    }

    @Test
    void invalidOutstandingInputsFail() {
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OutstandingChildOrderState(0)).getMessage().contains("capacity"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> outstanding.put(1, 1, 1)).getMessage().contains("slot"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> outstanding.put(0, 0, 1)).getMessage().contains("childOrderId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> outstanding.applyFill(0, -1)).getMessage().contains("fillQty"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> outstanding.childOrderId(1)).getMessage().contains("slot"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> outstanding.remainingQty(1)).getMessage().contains("slot"));
    }
}
