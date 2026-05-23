package com.nitroj.adaptive.quantum.sor.state;

import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link ChildOrderState}.
 *
 * <p>Role in system: verifies child lifecycle status and fill quantity storage.</p>
 *
 * <p>Relationships: complements outstanding quantity tracking in
 * {@link OutstandingChildOrderStateTest}.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: invalid filled quantities fail before stats aggregation
 * sees impossible values.</p>
 */
final class ChildOrderStateTest {
    @Test
    void updateStatusAndFilledQtyWork() {
        final ChildOrderState state = new ChildOrderState(1);
        state.update(0, OrderStatus.PARTIALLY_FILLED, 10);

        assertEquals(OrderStatus.PARTIALLY_FILLED, state.status(0));
        assertEquals(10, state.filledQty(0));
    }

    @Test
    void invalidChildStateInputsFail() {
        final ChildOrderState state = new ChildOrderState(1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ChildOrderState(0)).getMessage().contains("capacity"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> state.update(1, OrderStatus.NEW, 0)).getMessage().contains("slot"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> state.update(0, OrderStatus.NEW, -1)).getMessage().contains("filledQty"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> state.status(1)).getMessage().contains("slot"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> state.filledQty(1)).getMessage().contains("slot"));
    }
}
