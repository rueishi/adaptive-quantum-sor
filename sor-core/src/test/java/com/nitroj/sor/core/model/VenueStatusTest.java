package com.nitroj.sor.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link VenueStatus}.
 *
 * <p>Role in system: verifies venue status constants and availability helper
 * used by session state and routing guards.</p>
 *
 * <p>Relationships: isolated constants/helper test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: availability semantics should be explicit and stable.</p>
 */
final class VenueStatusTest {
    @Test
    void constantsAndAvailabilityWork() {
        assertEquals(0, VenueStatus.OPEN);
        assertEquals(1, VenueStatus.CLOSED);
        assertEquals(2, VenueStatus.OUTAGE);
        assertEquals(3, VenueStatus.DISABLED);
        assertTrue(VenueStatus.isAvailable(VenueStatus.OPEN));
        assertFalse(VenueStatus.isAvailable(VenueStatus.CLOSED));
        assertFalse(VenueStatus.isAvailable(VenueStatus.OUTAGE));
        assertFalse(VenueStatus.isAvailable(VenueStatus.DISABLED));
        assertTrue(VenueStatus.isValid(VenueStatus.OPEN));
        assertTrue(VenueStatus.isValid(VenueStatus.CLOSED));
        assertTrue(VenueStatus.isValid(VenueStatus.OUTAGE));
        assertTrue(VenueStatus.isValid(VenueStatus.DISABLED));
        assertFalse(VenueStatus.isValid(99));
    }
}
