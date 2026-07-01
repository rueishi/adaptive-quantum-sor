package com.nitroj.sor.core.intake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies order queue capacity validation behavior for ring-buffer intake for parent orders and inbound fills.
 *
 * <p>Run with :sor-core:test to protect hot-path queueing and backpressure tests.</p>
 */
class OrderQueueCapacityValidationTest {
    @ParameterizedTest
    @ValueSource(ints = {1000, 1023, 5000})
    void nonPowerOfTwoCapacityFailsBuild(final int capacity) {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> IntakeTestSupport.builder(capacity).build());

        assertTrue(ex.getMessage().contains("power-of-two"));
    }

    @Test
    void powerOfTwoCapacityBuilds() {
        assertDoesNotThrow(() -> IntakeTestSupport.builder(1024).build().close());
    }
}
