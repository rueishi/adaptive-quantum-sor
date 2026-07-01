package com.nitroj.sor.testkit.scenario;

/**
 * Responsibility: expose deterministic public results for a scenario run.
 *
 * <p>Role in system: Gradle/JUnit scenario tests compare summaries from two
 * runs instead of private simulator internals or random draw positions.</p>
 *
 * <p>Relationships: produced by {@link ScenarioRunner} and checked by
 * {@link ScenarioAssertions}.</p>
 *
 * <p>Lifecycle: immutable value created at the end of a scenario run.</p>
 *
 * <p>Design intent: primitive/string-backed fields make equality stable and
 * keep replay assertions straightforward.</p>
 */
public record ScenarioSummary(
        String scenarioId,
        long seed,
        int ticksRun,
        long bookChecksum,
        int orderCount,
        int childOrderCount,
        int outcomeCount,
        int routeCount,
        int rejectCount,
        int partialFillCount,
        int fullFillCount,
        long residualQty,
        long normalWindowMovementTicks,
        long volatileWindowMovementTicks,
        long normalWindowMedianQty,
        long thinBookWindowMedianQty,
        int staleEventCount,
        int outageEventCount,
        int detectedNormalCount,
        int detectedVolatileCount,
        int detectedThinBookCount
) {
    public ScenarioSummary {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        if (ticksRun < 0) {
            throw new IllegalArgumentException("ticksRun must be non-negative");
        }
    }
}
