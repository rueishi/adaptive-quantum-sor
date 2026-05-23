package com.nitroj.adaptive.quantum.sor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link ChildOrderBuffer}.
 *
 * <p>Role in system: verifies fixed-capacity child-order output behavior for
 * future executioners.</p>
 *
 * <p>Relationships: owns reusable {@link ChildOrder} slots, whose field
 * semantics are tested separately.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: overflow and reset behavior are explicit because this buffer
 * will sit near the hot path.</p>
 */
final class ChildOrderBufferTest {
    @Test
    void addGetSizeCapacityAndResetWork() {
        final ChildOrderBuffer buffer = new ChildOrderBuffer(1);

        assertEquals(1, buffer.capacity());
        assertEquals(0, buffer.size());
        final ChildOrder order = buffer.add(10, 1, 0, 2, Side.BUY, 50, 7, 99);
        assertEquals(order, buffer.get(0));
        assertEquals(1, buffer.size());
        assertEquals(7, order.policyVersion);
        assertEquals(99, order.policyHash64);

        buffer.reset();

        assertEquals(0, buffer.size());
        assertEquals(0, order.childOrderId);
    }

    @Test
    void invalidCapacityOverflowAndBadIndexFail() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ChildOrderBuffer(0)).getMessage().contains("capacity"));
        final ChildOrderBuffer buffer = new ChildOrderBuffer(1);
        buffer.add(10, 1, 0, 2, Side.BUY, 50, 7, 99);
        assertTrue(assertThrows(IllegalStateException.class, () -> buffer.add(11, 1, 0, 3, Side.BUY, 50, 7, 99)).getMessage().contains("buffer is full"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1)).getMessage().contains("child order index"));
    }
}
