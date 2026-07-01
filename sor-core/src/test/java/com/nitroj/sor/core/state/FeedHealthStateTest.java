package com.nitroj.sor.core.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link FeedHealthState}.
 *
 * <p>Role in system: verifies feed staleness and monotonic sequence tracking.</p>
 *
 * <p>Relationships: isolated guard-state test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: stale feed detection must be readable by later routing
 * guards.</p>
 */
final class FeedHealthStateTest {
    @Test
    void updateAndAccessorsWork() {
        final FeedHealthState feed = new FeedHealthState(1);
        feed.update(0, 10, true);

        assertTrue(feed.isStale(0));
        assertEquals(10, feed.lastSequence(0));
    }

    @Test
    void invalidFeedInputsFail() {
        final FeedHealthState feed = new FeedHealthState(1);
        feed.update(0, 10, true);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new FeedHealthState(0)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> feed.update(0, 9, false)).getMessage().contains("sequence"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> feed.isStale(1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> feed.lastSequence(1)).getMessage().contains("venueId"));
    }
}
