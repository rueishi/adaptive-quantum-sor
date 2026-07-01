package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: unit test {@link RegimeId}.
 *
 * <p>Role in system: verifies the dense regime IDs used by route keys and
 * future regime detection.</p>
 *
 * <p>Relationships: isolated constants test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: fixed numeric values are part of the array-index contract.</p>
 */
final class RegimeIdTest {
    @Test
    void constantsMatchDenseIds() {
        assertEquals(0, RegimeId.NORMAL);
        assertEquals(1, RegimeId.VOLATILE);
        assertEquals(2, RegimeId.THIN_BOOK);
        assertEquals(3, RegimeId.STRESSED);
        assertEquals(4, RegimeId.AUCTION);
    }
}
