package com.nitroj.adaptive.quantum.sor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: unit test {@link OrderStatus}.
 *
 * <p>Role in system: verifies primitive lifecycle status values consumed by
 * child/parent state containers.</p>
 *
 * <p>Relationships: isolated constants test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: direct assertions preserve the status-code contract.</p>
 */
final class OrderStatusTest {
    @Test
    void constantsMatchLifecycleCodes() {
        assertEquals(0, OrderStatus.NEW);
        assertEquals(1, OrderStatus.ACKED);
        assertEquals(2, OrderStatus.PARTIALLY_FILLED);
        assertEquals(3, OrderStatus.FILLED);
        assertEquals(4, OrderStatus.REJECTED);
        assertEquals(5, OrderStatus.CANCELLED);
        assertEquals(6, OrderStatus.NO_ACTIVE_POLICY);
        assertEquals(7, OrderStatus.NO_LIQUIDITY);
    }
}
