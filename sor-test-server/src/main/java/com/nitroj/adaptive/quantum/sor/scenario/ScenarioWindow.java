package com.nitroj.adaptive.quantum.sor.scenario;

/**
 * Responsibility: define one inclusive tick window and the regime active
 * during that window.
 *
 * <p>Role in system: {@link ScenarioSpec} uses windows to make scenario regime
 * schedules explicit and replayable.</p>
 *
 * <p>Relationships: consumed by {@link ScenarioSpec#regimeAt(int)} and tests
 * that assert boundary behavior.</p>
 *
 * <p>Lifecycle: immutable after construction.</p>
 *
 * <p>Design intent: explicit start/end ticks prevent hidden wall-clock or
 * probabilistic regime transitions in deterministic scenario tests.</p>
 */
public record ScenarioWindow(int startTickInclusive, int endTickInclusive, int regimeId) {
    public ScenarioWindow {
        if (startTickInclusive < 0) {
            throw new IllegalArgumentException("startTickInclusive must be non-negative");
        }
        if (endTickInclusive < startTickInclusive) {
            throw new IllegalArgumentException("endTickInclusive must be >= startTickInclusive");
        }
        if (regimeId < 0) {
            throw new IllegalArgumentException("regimeId must be non-negative");
        }
    }

    /**
     * Returns whether this window owns the supplied tick.
     */
    public boolean contains(final int tick) {
        return tick >= startTickInclusive && tick <= endTickInclusive;
    }
}
