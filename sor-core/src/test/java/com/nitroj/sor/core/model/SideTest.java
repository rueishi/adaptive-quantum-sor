package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link Side}.
 *
 * <p>Role in system: verifies dense side constants and validation helper used
 * by parent and child order structures.</p>
 *
 * <p>Relationships: isolated constants test; order classes test how these
 * constants are consumed.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: constants are directly asserted to satisfy the project's
 * public-surface coverage standard.</p>
 */
final class SideTest {
    @Test
    void constantsAndValidationWork() {
        assertEquals(1, Side.BUY);
        assertEquals(2, Side.SELL);
        assertTrue(Side.isValid(Side.BUY));
        assertTrue(Side.isValid(Side.SELL));
        assertFalse(Side.isValid(0));
        assertFalse(Side.isValid(99));
    }
}
