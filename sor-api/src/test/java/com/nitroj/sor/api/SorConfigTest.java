package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the public SOR configuration contract.
 *
 * <p>Role in system: protects the zero-dependency API configuration used by
 * `SorEngineBuilder` and the engine-owned market-book sizing introduced in
 * Phase 9.</p>
 *
 * <p>Relationships: covers {@link SorConfig} independently of `sor-core` so
 * embedders see validation failures at the public boundary.</p>
 *
 * <p>Lifecycle: unit test run by `:sor-api:test` whenever the public API
 * module is validated.</p>
 *
 * <p>Design intent: keep compatibility with the original two-argument
 * constructor while making instrument/venue dimensions and destructive reset
 * enablement explicit.</p>
 */
class SorConfigTest {
    /**
     * Confirms the legacy constructor remains available and creates a minimal
     * 1x1 market-book configuration.
     */
    @Test
    void legacyConstructorDefaultsMarketBookDimensions() {
        final SorConfig config = new SorConfig(128, 250);

        assertEquals(128, config.orderQueueCapacity());
        assertEquals(250, config.closeDrainTimeoutMillis());
        assertEquals(1, config.instrumentCount());
        assertEquals(1, config.venueCount());
        assertEquals(false, config.destructiveResetEnabled());
    }

    /**
     * Confirms the full constructor carries engine-owned market-book
     * dimensions.
     */
    @Test
    void fullConstructorCarriesMarketBookDimensions() {
        final SorConfig config = new SorConfig(128, 250, 3, 4, true);

        assertEquals(3, config.instrumentCount());
        assertEquals(4, config.venueCount());
        assertEquals(true, config.destructiveResetEnabled());
    }

    /**
     * Confirms invalid market-book dimensions fail at the public API boundary.
     */
    @Test
    void rejectsInvalidMarketBookDimensions() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 0, 0, 1)).getMessage().contains("instrumentCount"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 0, 1, 0)).getMessage().contains("venueCount"));
    }
}
