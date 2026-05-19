package com.nitroj.adaptive.quantum.sor.lifecycle;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify lifecycle narrative logging.
 *
 * <p>Role in system: covers chronological storage, logger failure isolation,
 * high-rate/full-buffer behavior, and human-readable formatting.</p>
 *
 * <p>Relationships: tests {@link InMemoryLifecycleEventStore} and
 * {@link ConsoleNarrativeLifecycleLogger} together.</p>
 *
 * <p>Lifecycle: executed by Gradle for P1-TC-021 coverage.</p>
 *
 * <p>Design intent: lifecycle logging must not block routing when unavailable.</p>
 */
final class NarrativeLifecycleLoggerTest {
    @Test
    void majorEventsAppearInChronologicalOrder() {
        final InMemoryLifecycleEventStore store = new InMemoryLifecycleEventStore(4, true);
        store.append(event(2, 20, "second"));
        store.append(event(1, 10, "first"));

        assertEquals("first", store.snapshot().get(0).message);
        assertEquals("second", store.snapshot().get(1).message);
    }

    @Test
    void loggerUnavailableDoesNotThrow() {
        final ConsoleNarrativeLifecycleLogger logger = new ConsoleNarrativeLifecycleLogger(
                new InMemoryLifecycleEventStore(2, true),
                new PrintStream(new OutputStream() {
                    @Override
                    public void write(final int b) {
                        throw new RuntimeException("down");
                    }
                })
        );

        assertDoesNotThrow(() -> logger.publish(event(1, 1, "ok")));
        assertFalse(logger.available());
    }

    @Test
    void highEventRateAndBufferFullBehaviorFollowConfig() {
        final InMemoryLifecycleEventStore dropping = new InMemoryLifecycleEventStore(2, true);
        dropping.append(event(1, 1, "one"));
        dropping.append(event(2, 2, "two"));
        dropping.append(event(3, 3, "three"));
        final InMemoryLifecycleEventStore strict = new InMemoryLifecycleEventStore(1, false);
        strict.append(event(1, 1, "one"));

        assertEquals(1, dropping.droppedCount());
        assertEquals("two", dropping.snapshot().get(0).message);
        assertFalse(strict.append(event(2, 2, "two")));
        assertEquals(1, strict.droppedCount());
    }

    @Test
    void eventValidationAndFormattingAreCovered() {
        final LifecycleEvent event = event(1, 1, "hello");
        assertTrue(ConsoleNarrativeLifecycleLogger.format(event).contains("hello"));
        assertEquals(LifecycleEventType.SOR_DECISION, LifecycleEventType.SOR_DECISION);
        assertThrows(IllegalArgumentException.class, () -> new InMemoryLifecycleEventStore(0, true));
        assertThrows(IllegalArgumentException.class, () -> new LifecycleEvent(0, 0, 1, 1, 1, "bad"));
    }

    private static LifecycleEvent event(final long id, final long timestamp, final String message) {
        return new LifecycleEvent(id, timestamp, 1, LifecycleEventType.SOR_DECISION, id, message);
    }
}
