package com.nitroj.adaptive.quantum.sor.scenario;

/**
 * Responsibility: define live scenario reset modes for notebook/API runs.
 *
 * <p>Role in system: separates isolated Gradle/JUnit replay from live demo
 * control-plane behavior that may purge or append to state.</p>
 *
 * <p>Relationships: used by {@link ScenarioResetRequest},
 * {@link ScenarioControlService}, and API handlers.</p>
 *
 * <p>Lifecycle: stable enum for Phase 5 live scenario controls.</p>
 *
 * <p>Design intent: make destructive live-state changes explicit and make
 * APPEND visibly non-replay-safe.</p>
 */
public enum ScenarioResetMode {
    PURGE_AND_REPOPULATE(true),
    KEEP_POLICY_PURGE_STATS(true),
    ISOLATED(true),
    APPEND(false);

    private final boolean replaySafe;

    ScenarioResetMode(final boolean replaySafe) {
        this.replaySafe = replaySafe;
    }

    public boolean replaySafe() {
        return replaySafe;
    }

    public static ScenarioResetMode parse(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("resetMode must not be blank");
        }
        for (ScenarioResetMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("resetMode must be PURGE_AND_REPOPULATE, KEEP_POLICY_PURGE_STATS, ISOLATED, or APPEND");
    }
}
