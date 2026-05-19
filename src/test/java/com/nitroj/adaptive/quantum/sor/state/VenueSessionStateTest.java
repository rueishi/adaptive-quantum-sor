package com.nitroj.adaptive.quantum.sor.state;

import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link VenueSessionState}.
 *
 * <p>Role in system: verifies venue availability guards used by routing and
 * policy compilation.</p>
 *
 * <p>Relationships: consumes {@link VenueStatus} constants.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: default-open and outage behavior are both explicitly
 * covered.</p>
 */
final class VenueSessionStateTest {
    @Test
    void statusAndAvailabilityWork() {
        final VenueSessionState sessions = new VenueSessionState(1);

        assertEquals(VenueStatus.OPEN, sessions.status(0));
        assertTrue(sessions.isAvailable(0));
        sessions.setStatus(0, VenueStatus.OUTAGE);
        assertEquals(VenueStatus.OUTAGE, sessions.status(0));
        assertFalse(sessions.isAvailable(0));
    }

    @Test
    void invalidVenueFails() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueSessionState(0)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> new VenueSessionState(1).status(1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> new VenueSessionState(1).setStatus(1, VenueStatus.OPEN)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueSessionState(1).setStatus(0, 99)).getMessage().contains("known VenueStatus"));
    }
}
