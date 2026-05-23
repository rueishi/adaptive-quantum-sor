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
 */
public record SorConfig(int orderQueueCapacity, long closeDrainTimeoutMillis) {
    public SorConfig {
        if (orderQueueCapacity <= 0) {
            throw new IllegalArgumentException("orderQueueCapacity must be positive");
        }
        if (closeDrainTimeoutMillis < 0) {
            throw new IllegalArgumentException("closeDrainTimeoutMillis must be non-negative");
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
        return new SorConfig(1024, 5000);
    }
}
