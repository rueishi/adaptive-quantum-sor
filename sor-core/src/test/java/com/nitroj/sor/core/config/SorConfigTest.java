package com.nitroj.sor.core.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link SorConfig} and its runtime mode enum.
 *
 * <p>Role in system: verifies constructor validation, generated record
 * accessors, default values, and mode parsing.</p>
 *
 * <p>Relationships: independent from {@link ConfigLoader}; this test treats the
 * config record as a direct value object.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: generated record accessors are intentionally asserted
 * directly because the project standard requires public method coverage.</p>
 */
final class SorConfigTest {

    @Test
    void constructorStoresAccessorValues() {
        final SorConfig config = new SorConfig(1, 2, 3, 4, SorConfig.RuntimeMode.STRICT, false);

        assertEquals(1, config.instrumentCount());
        assertEquals(2, config.venueCount());
        assertEquals(3, config.regimeCount());
        assertEquals(4, config.urgencyCount());
        assertEquals(SorConfig.RuntimeMode.STRICT, config.runtimeMode());
        assertEquals(false, config.strictUnknownKeys());
    }

    @Test
    void defaultsReturnExpectedValues() {
        final SorConfig defaults = SorConfig.defaults();

        assertEquals(20, defaults.instrumentCount());
        assertEquals(100, defaults.venueCount());
        assertEquals(5, defaults.regimeCount());
        assertEquals(4, defaults.urgencyCount());
        assertEquals(SorConfig.RuntimeMode.DEMO, defaults.runtimeMode());
        assertTrue(defaults.strictUnknownKeys());
    }

    @Test
    void constructorRejectsInvalidValues() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(0, 1, 1, 1, SorConfig.RuntimeMode.DEMO, true)).getMessage().contains("instrumentCount"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 0, 1, 1, SorConfig.RuntimeMode.DEMO, true)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 1, 0, 1, SorConfig.RuntimeMode.DEMO, true)).getMessage().contains("regimeCount"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 1, 1, 0, SorConfig.RuntimeMode.DEMO, true)).getMessage().contains("urgencyCount"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorConfig(1, 1, 1, 1, null, true)).getMessage().contains("runtimeMode"));
    }

    @Test
    void runtimeModeParsesValidAndInvalidValues() {
        assertEquals(SorConfig.RuntimeMode.DEMO, SorConfig.RuntimeMode.parse("demo"));
        assertEquals(SorConfig.RuntimeMode.STRICT, SorConfig.RuntimeMode.parse("STRICT"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> SorConfig.RuntimeMode.parse("turbo")).getMessage().contains("runtime.mode"));
    }
}
