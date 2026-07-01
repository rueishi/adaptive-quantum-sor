package com.nitroj.sor.core.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link VenueThrottleState}.
 *
 * <p>Role in system: verifies venue order-rate throttle storage.</p>
 *
 * <p>Relationships: isolated guard-state test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: negative throttles are invalid because they would make
 * routing guards ambiguous.</p>
 */
final class VenueThrottleStateTest {
    @Test
    void setAndGetRateWork() {
        final VenueThrottleState throttle = new VenueThrottleState(1);
        throttle.setMaxOrderRatePerSecond(0, 100);

        assertEquals(100, throttle.maxOrderRatePerSecond(0));
    }

    @Test
    void invalidThrottleInputsFail() {
        final VenueThrottleState throttle = new VenueThrottleState(1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueThrottleState(0)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> throttle.setMaxOrderRatePerSecond(0, -1)).getMessage().contains("rate"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> throttle.maxOrderRatePerSecond(1)).getMessage().contains("venueId"));
    }
}
