package com.nitroj.sor.testkit.scenario;

/**
 * Responsibility: hold bounded default stochastic parameters for scenario
 * simulation.
 *
 * <p>Role in system: Phase 5 scenario contracts need one validated surface for
 * volatility, spread, quantity, probability, and order-flow defaults before the
 * low-level simulators consume richer scenario state.</p>
 *
 * <p>Relationships: referenced by {@link ScenarioSpec}; later simulator task
 * cards read these values when generating regime-aware market data and order
 * flow.</p>
 *
 * <p>Lifecycle: immutable after construction and usually created from defaults
 * or validated configuration at scenario startup.</p>
 *
 * <p>Design intent: keep Phase 5 defaults explicit and integer-based so tests
 * can make deterministic assertions without floating-point or external
 * calibration dependencies.</p>
 */
public record ScenarioSimulatorConfig(
        int normalVolatilityTicks,
        int volatileVolatilityTicks,
        int thinBookVolatilityTicks,
        int baseSpreadTicks,
        long normalDisplayedQty,
        long thinBookDisplayedQty,
        int staleProbabilityBps,
        int outageProbabilityBps,
        int parentArrivalProbabilityBps
) {
    public ScenarioSimulatorConfig {
        requireNonNegative("normalVolatilityTicks", normalVolatilityTicks);
        requireNonNegative("volatileVolatilityTicks", volatileVolatilityTicks);
        requireNonNegative("thinBookVolatilityTicks", thinBookVolatilityTicks);
        requirePositive("baseSpreadTicks", baseSpreadTicks);
        requireNonNegative("normalDisplayedQty", normalDisplayedQty);
        requireNonNegative("thinBookDisplayedQty", thinBookDisplayedQty);
        requireBps("staleProbabilityBps", staleProbabilityBps);
        requireBps("outageProbabilityBps", outageProbabilityBps);
        requireBps("parentArrivalProbabilityBps", parentArrivalProbabilityBps);
    }

    /**
     * Returns deterministic demo defaults used when no scenario-specific config
     * is supplied.
     */
    public static ScenarioSimulatorConfig defaults() {
        return new ScenarioSimulatorConfig(1, 8, 3, 2, 5_000L, 500L, 100, 50, 5_000);
    }

    private static void requirePositive(final String name, final int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(final String name, final int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireNonNegative(final String name, final long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requireBps(final String name, final int value) {
        if (value < 0 || value > 10_000) {
            throw new IllegalArgumentException(name + " must be between 0 and 10000");
        }
    }
}
