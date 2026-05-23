package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the P8-03 builder shell rejects incomplete engine
 * configuration.
 *
 * <p>Role in system: P8-03 cannot construct a real engine because `sor-core`
 * and SPI wiring land in later cards, but it must establish that missing
 * dependencies are explicit.</p>
 *
 * <p>Relationships: this test is expanded in P8-04/P8-06 when typed SPI
 * setters and implementation construction exist.</p>
 *
 * <p>Lifecycle: creates a new builder and immediately attempts to build.</p>
 *
 * <p>Design intent: prevent accidental fake defaults in the public construction
 * surface.</p>
 */
class SorEngineBuilderRejectionTest {
    /**
     * Confirms every required dependency name appears in the rejection message.
     */
    @Test
    void buildRejectsMissingDependenciesByName() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> SorEngineBuilder.create().build());

        assertTrue(exception.getMessage().contains("config"));
        assertTrue(exception.getMessage().contains("marketData"));
        assertTrue(exception.getMessage().contains("venueAdapter"));
        assertTrue(exception.getMessage().contains("riskProvider"));
        assertTrue(exception.getMessage().contains("persistence"));
        assertTrue(exception.getMessage().contains("clock"));
    }
}
