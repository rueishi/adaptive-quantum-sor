package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link ParentOrderIntentQueue}.
 *
 * <p>Role in system: verifies fixed-capacity FIFO behavior for parent order
 * intake.</p>
 *
 * <p>Relationships: consumes {@link OrderIntent} values, whose validation is
 * tested separately.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: queue wraparound is covered as an edge case because ring
 * buffers are prone to off-by-one errors.</p>
 */
final class ParentOrderIntentQueueTest {
    @Test
    void offersPollsAndReportsCapacity() {
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(1);
        final OrderIntent intent = new OrderIntent(1, 0, Side.BUY, 100, 0, 10);

        assertEquals(1, queue.capacity());
        assertEquals(0, queue.size());
        assertTrue(queue.offer(intent));
        assertEquals(1, queue.size());
        assertFalse(queue.offer(new OrderIntent(2, 0, Side.SELL, 100, 0, 11)));
        assertEquals(intent, queue.poll());
        assertEquals(0, queue.size());
        assertNull(queue.poll());
    }

    @Test
    void wraparoundPreservesFifoOrder() {
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(2);
        final OrderIntent first = new OrderIntent(1, 0, Side.BUY, 10, 0, 0);
        final OrderIntent second = new OrderIntent(2, 0, Side.SELL, 20, 0, 0);
        final OrderIntent third = new OrderIntent(3, 0, Side.BUY, 30, 0, 0);

        assertTrue(queue.offer(first));
        assertTrue(queue.offer(second));
        assertEquals(first, queue.poll());
        assertTrue(queue.offer(third));
        assertEquals(second, queue.poll());
        assertEquals(third, queue.poll());
    }

    @Test
    void invalidConstructionOrOfferFails() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ParentOrderIntentQueue(0)).getMessage().contains("capacity"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new ParentOrderIntentQueue(1).offer(null)).getMessage().contains("intent"));
    }
}
