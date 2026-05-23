package com.nitroj.adaptive.quantum.sor.lifecycle;

/**
 * Responsibility: define lifecycle event publication.
 *
 * <p>Role in system: components publish lifecycle events without knowing
 * whether they are printed, buffered, streamed, or dropped under pressure.</p>
 *
 * <p>Relationships: implemented by {@link ConsoleNarrativeLifecycleLogger}.</p>
 *
 * <p>Lifecycle: long-lived process service, used by simulators/API/policy code.</p>
 *
 * <p>Design intent: failures in narrative logging should not stop routing.</p>
 */
public interface NarrativeLifecycleLogger {
    /** Publishes a lifecycle event. */
    void publish(LifecycleEvent event);
}
