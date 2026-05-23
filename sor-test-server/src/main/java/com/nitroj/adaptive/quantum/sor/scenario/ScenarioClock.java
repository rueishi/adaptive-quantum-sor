package com.nitroj.adaptive.quantum.sor.scenario;

/**
 * Responsibility: provide deterministic simulated time for scenario runs.
 *
 * <p>Role in system: scenario-driven tests must not depend on wall-clock time,
 * sleeps, or scheduler timing. This clock exposes tick-derived timestamps.</p>
 *
 * <p>Relationships: used by {@link ScenarioRunner}, order-flow simulators, and
 * later live scenario APIs.</p>
 *
 * <p>Lifecycle: created at scenario start and advanced once per simulated tick.</p>
 *
 * <p>Design intent: timestamps become simple functions of start nanos, tick
 * index, and nanos per tick, which makes replay exact.</p>
 */
public final class ScenarioClock {
    private final long startNanos;
    private final long nanosPerTick;
    private int tick;

    public ScenarioClock(final long startNanos, final long nanosPerTick) {
        if (startNanos < 0) {
            throw new IllegalArgumentException("startNanos must be non-negative");
        }
        if (nanosPerTick <= 0) {
            throw new IllegalArgumentException("nanosPerTick must be positive");
        }
        this.startNanos = startNanos;
        this.nanosPerTick = nanosPerTick;
    }

    public int tick() {
        return tick;
    }

    public long nowNanos() {
        return startNanos + tick * nanosPerTick;
    }

    public void advance() {
        tick++;
    }
}
