package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: unit test {@link UrgencyId}.
 *
 * <p>Role in system: verifies dense urgency IDs used by route-key indexing and
 * future policy behavior.</p>
 *
 * <p>Relationships: isolated constants test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: fixed IDs make route lists stable and deterministic.</p>
 */
final class UrgencyIdTest {
    @Test
    void constantsMatchDenseIds() {
        assertEquals(0, UrgencyId.LOW);
        assertEquals(1, UrgencyId.NORMAL);
        assertEquals(2, UrgencyId.HIGH);
        assertEquals(3, UrgencyId.IMMEDIATE);
    }
}
