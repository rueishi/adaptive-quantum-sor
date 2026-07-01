package com.nitroj.sor.api;

/**
 * Responsibility: minimal public engine configuration handle for
 * {@link SorEngineBuilder}.
 *
 * <p>Role in system: P8-04 gives the builder a zero-dependency public config
 * type. `sor-core` may translate this into richer internal configuration in
 * later cards.</p>
 *
 * <p>Relationships: supplied to {@link SorEngineBuilder#config(SorConfig)}.</p>
 *
 * <p>Lifecycle: immutable value created during engine wiring.</p>
 *
 * <p>Design intent: expose only framework-level lifecycle settings while
 * retaining freedom for internal config evolution.</p>
 *
 * @param orderQueueCapacity desired parent order intake capacity
 * @param closeDrainTimeoutMillis graceful close drain timeout
 * @param instrumentCount number of dense instruments tracked by the engine
 * @param venueCount number of dense venues tracked by the engine
 * @param destructiveResetEnabled whether destructive control-plane resets are enabled
 */
public record SorConfig(
        int orderQueueCapacity,
        long closeDrainTimeoutMillis,
        int instrumentCount,
        int venueCount,
        boolean destructiveResetEnabled
) {
    /**
     * Creates configuration with one instrument, one venue, and destructive
     * resets disabled.
     *
     * @param orderQueueCapacity desired parent order intake capacity
     * @param closeDrainTimeoutMillis graceful close drain timeout
     */
    public SorConfig(final int orderQueueCapacity, final long closeDrainTimeoutMillis) {
        this(orderQueueCapacity, closeDrainTimeoutMillis, 1, 1, false);
    }

    /**
     * Creates configuration with destructive resets disabled.
     *
     * @param orderQueueCapacity desired parent order intake capacity
     * @param closeDrainTimeoutMillis graceful close drain timeout
     * @param instrumentCount number of dense instruments tracked by the engine
     * @param venueCount number of dense venues tracked by the engine
     */
    public SorConfig(final int orderQueueCapacity, final long closeDrainTimeoutMillis,
                     final int instrumentCount, final int venueCount) {
        this(orderQueueCapacity, closeDrainTimeoutMillis, instrumentCount, venueCount, false);
    }

    /**
     * Validates the immutable configuration.
     */
    public SorConfig {
        if (orderQueueCapacity <= 0) {
            throw new IllegalArgumentException("orderQueueCapacity must be positive");
        }
        if (closeDrainTimeoutMillis < 0) {
            throw new IllegalArgumentException("closeDrainTimeoutMillis must be non-negative");
        }
        if (instrumentCount <= 0) {
            throw new IllegalArgumentException("instrumentCount must be positive");
        }
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
    }

    /**
     * Creates the conservative default configuration.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return default API config
     */
    public static SorConfig defaults() {
        return new SorConfig(1024, 5000, 1, 1, false);
    }
}
