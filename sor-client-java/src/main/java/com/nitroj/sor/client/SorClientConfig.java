package com.nitroj.sor.client;

import java.time.Duration;

/**
 * Immutable Java SDK connection settings.
 *
 * @param warmupOrderCount number of synthetic orders used when {@link AeronSorClient#warmup()} is called
 * @param connectTimeout maximum time an integrator should allow for connection establishment
 */
public record SorClientConfig(int warmupOrderCount, Duration connectTimeout) {
    /**
     * Creates validated client configuration.
     *
     * @param warmupOrderCount non-negative synthetic warmup order count
     * @param connectTimeout non-null connection timeout
     */
    public SorClientConfig {
        if (warmupOrderCount < 0) {
            throw new IllegalArgumentException("warmupOrderCount must be non-negative");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
    }

    /**
     * Returns conservative SDK defaults.
     *
     * @return default client configuration
     */
    public static SorClientConfig defaults() {
        return new SorClientConfig(0, Duration.ofSeconds(5));
    }
}
