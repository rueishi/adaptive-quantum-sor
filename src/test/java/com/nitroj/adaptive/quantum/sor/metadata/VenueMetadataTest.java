package com.nitroj.adaptive.quantum.sor.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link VenueMetadata}.
 *
 * <p>Role in system: verifies dense venue metadata and instrument support
 * lookup.</p>
 *
 * <p>Relationships: independent from order-type capabilities, which have their
 * own matrix test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: instrument support shape is validated because later policy
 * linting relies on exact IV dimensions.</p>
 */
final class VenueMetadataTest {
    @Test
    void simulatedMetadataAndAccessorsWork() {
        final VenueMetadata venues = VenueMetadata.simulated(2, 3);

        assertEquals("VENUE2", venues.venueName(2));
        assertTrue(venues.isEnabled(1));
        assertEquals(3, venues.venueCount());
        assertTrue(venues.supportsInstrument(1, 2));
    }

    @Test
    void invalidVenueMetadataFails() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> VenueMetadata.simulated(1, 0)).getMessage().contains("venueNames"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueMetadata(1, new String[]{"V"}, new boolean[]{true}, new boolean[0])).getMessage().contains("supportsInstrument"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueMetadata(1, new String[]{" "}, new boolean[]{true}, new boolean[]{true})).getMessage().contains("venue name"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> VenueMetadata.simulated(1, 1).venueName(1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> VenueMetadata.simulated(1, 1).isEnabled(1)).getMessage().contains("venueId"));
    }
}
