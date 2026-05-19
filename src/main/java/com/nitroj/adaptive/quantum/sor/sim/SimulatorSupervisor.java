package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;

/**
 * Responsibility: execute simulator tasks behind a failure guard.
 *
 * <p>Role in system: converts unexpected simulator task exceptions into
 * observable health state and lifecycle events, satisfying the Phase 1
 * simulator-thread failure contract without requiring a background scheduler.</p>
 *
 * <p>Relationships: writes {@link SimulatorHealthState} and
 * {@link InMemoryLifecycleEventStore}. Individual simulator classes can be
 * wrapped by this supervisor from tests, startup, or a future scheduler.</p>
 *
 * <p>Lifecycle: created with the simulator runtime and reused for guarded
 * simulator invocations. Recovery is explicit through the health state.</p>
 *
 * <p>Design intent: keep exception handling centralized so failed simulators
 * cannot leave dependent optimizer publication paths believing state is healthy.</p>
 */
public final class SimulatorSupervisor {
    private final SimulatorHealthState healthState;
    private final InMemoryLifecycleEventStore lifecycleEvents;
    private long nextEventId = 1L;

    public SimulatorSupervisor(
            final SimulatorHealthState healthState,
            final InMemoryLifecycleEventStore lifecycleEvents
    ) {
        if (healthState == null || lifecycleEvents == null) {
            throw new IllegalArgumentException("healthState and lifecycleEvents must not be null");
        }
        this.healthState = healthState;
        this.lifecycleEvents = lifecycleEvents;
    }

    /**
     * Runs one simulator task, catching unexpected runtime failures. Successful
     * tasks leave health untouched. Failed tasks mark simulator health failed,
     * append a lifecycle event, and return {@code false}.
     *
     * @param simulatorName logical simulator/component name
     * @param timestampNanos simulated or monotonic timestamp
     * @param task simulator work to execute
     * @return true when task completed, false when it failed and was recorded
     */
    public boolean runGuarded(final String simulatorName, final long timestampNanos, final Runnable task) {
        if (simulatorName == null || simulatorName.isBlank()) {
            throw new IllegalArgumentException("simulatorName must not be blank");
        }
        if (timestampNanos < 0) {
            throw new IllegalArgumentException("timestampNanos must be non-negative");
        }
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        try {
            task.run();
            return true;
        } catch (RuntimeException ex) {
            final String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            healthState.markFailed(simulatorName, message, timestampNanos);
            lifecycleEvents.append(new LifecycleEvent(
                    nextEventId++,
                    timestampNanos,
                    1,
                    LifecycleEventType.SIMULATOR_FAILURE,
                    healthState.failureCount(),
                    "simulator failed " + simulatorName + ": " + message
            ));
            return false;
        }
    }
}
