package com.nitroj.adaptive.quantum.sor.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link VenueBehaviorState}.
 *
 * <p>Role in system: verifies bounded simulated venue behavior profiles.</p>
 *
 * <p>Relationships: isolated from feature aggregation, which later consumes
 * this state.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: bps bounds are tested because model inputs must stay sane.</p>
 */
final class VenueBehaviorStateTest {
    @Test
    void updateAndToxicityAccessorWork() {
        final VenueBehaviorState behavior = new VenueBehaviorState(1);
        behavior.update(0, 100, 200, 9_000);

        assertEquals(100, behavior.toxicityBps(0));
    }

    @Test
    void invalidBehaviorInputsFail() {
        final VenueBehaviorState behavior = new VenueBehaviorState(1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new VenueBehaviorState(0)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> behavior.toxicityBps(1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> behavior.update(0, 10_001, 0, 0)).getMessage().contains("toxicityBps"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> behavior.update(0, 0, -1, 0)).getMessage().contains("rejectRateBps"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> behavior.update(0, 0, 0, 10_001)).getMessage().contains("fillProbabilityBps"));
    }
}
