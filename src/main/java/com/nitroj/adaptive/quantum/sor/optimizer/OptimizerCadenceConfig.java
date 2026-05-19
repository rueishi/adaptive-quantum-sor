package com.nitroj.adaptive.quantum.sor.optimizer;

/**
 * Responsibility: configure optimizer-cycle cadence.
 *
 * <p>Role in system: {@link PolicyOptimizerCoordinator} uses this immutable
 * value to decide whether a warm-path optimization cycle may run at a given
 * timestamp.</p>
 *
 * <p>Relationships: coordinator tests and future startup config create this
 * object before scheduling optimizer cycles.</p>
 *
 * <p>Lifecycle: constructed once and reused for coordinator invocations.</p>
 *
 * <p>Design intent: keep cadence validation explicit and fail fast on invalid
 * zero/negative periods.</p>
 */
public final class OptimizerCadenceConfig {
    public final long cycleIntervalNanos;

    public OptimizerCadenceConfig(final long cycleIntervalNanos) {
        if (cycleIntervalNanos <= 0) {
            throw new IllegalArgumentException("cycleIntervalNanos must be positive");
        }
        this.cycleIntervalNanos = cycleIntervalNanos;
    }
}
