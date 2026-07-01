package com.nitroj.sor.api.spi;

/**
 * Responsibility: injected time source for engine and adapter code.
 *
 * <p>Role in system: prevents direct calls to system time from framework hot
 * paths and enables deterministic simulator clocks.</p>
 *
 * <p>Relationships: supplied through {@code SorEngineBuilder.clock(...)}.</p>
 *
 * <p>Lifecycle: one clock is configured per engine instance.</p>
 *
 * <p>Design intent: make time ownership explicit and testable.</p>
 */
public interface Clock {
    /**
     * Returns monotonic nanoseconds from this clock.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Must return within
     * 100 nanoseconds for the system implementation.</p>
     *
     * @return monotonic nanoseconds
     */
    long nanoTime();

    /**
     * Returns wall-clock epoch nanoseconds.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Must return within
     * 100 nanoseconds for the system implementation.</p>
     *
     * @return wall-clock epoch nanoseconds
     */
    long epochNanos();

    /**
     * Creates the default system-backed clock.
     *
     * <p>Control-plane method, not hot-path. The returned clock methods are
     * hot-path-safe.</p>
     *
     * @return default system-backed clock
     */
    static Clock systemNano() {
        return SystemClock.INSTANCE;
    }
}
