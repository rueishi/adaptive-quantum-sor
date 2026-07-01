package com.nitroj.sor.core.lifecycle;

import java.io.PrintStream;

/**
 * Responsibility: publish lifecycle events to memory and optionally console.
 *
 * <p>Role in system: provides the Phase 1 human-readable story log while
 * keeping failures non-fatal for routing.</p>
 *
 * <p>Relationships: writes {@link InMemoryLifecycleEventStore} and can mirror
 * event messages to a {@link PrintStream}.</p>
 *
 * <p>Lifecycle: created at startup; safe to reuse for all lifecycle events.</p>
 *
 * <p>Design intent: `publish` catches runtime output failures so logger
 * unavailability does not stop the engine.</p>
 */
public final class ConsoleNarrativeLifecycleLogger implements NarrativeLifecycleLogger {
    private final InMemoryLifecycleEventStore store;
    private final PrintStream output;
    private boolean available = true;

    public ConsoleNarrativeLifecycleLogger(final InMemoryLifecycleEventStore store, final PrintStream output) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
        this.output = output;
    }

    @Override
    public void publish(final LifecycleEvent event) {
        try {
            store.append(event);
            if (output != null) {
                output.println(format(event));
            }
        } catch (RuntimeException ex) {
            available = false;
        }
    }

    public boolean available() {
        return available;
    }

    /** Formats an event outside the execution hot path. */
    public static String format(final LifecycleEvent event) {
        return event.timestampNanos + " type=" + event.eventType + " corr=" + event.correlationId + " " + event.message;
    }
}
