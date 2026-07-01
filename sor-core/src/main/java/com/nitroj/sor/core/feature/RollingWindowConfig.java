package com.nitroj.sor.core.feature;

/**
 * Responsibility: configure the simple Phase 1 rolling aggregation window.
 *
 * <p>Role in system: {@link FeatureAggregator} uses this object to decide how
 * far back in the outcome store to read and what market thresholds imply
 * volatile or thin-book regimes.</p>
 *
 * <p>Relationships: passed to feature aggregation and regime detection during
 * test/scenario setup.</p>
 *
 * <p>Lifecycle: immutable after construction and shared across aggregation
 * passes.</p>
 *
 * <p>Design intent: keep Phase 1 deterministic while making threshold behavior
 * explicit and testable.</p>
 */
public final class RollingWindowConfig {
    public final int maxOutcomeWindow;
    public final long thinBookQuantityThreshold;
    public final long volatileSpreadThresholdTicks;

    public RollingWindowConfig(
            final int maxOutcomeWindow,
            final long thinBookQuantityThreshold,
            final long volatileSpreadThresholdTicks
    ) {
        if (maxOutcomeWindow <= 0) {
            throw new IllegalArgumentException("maxOutcomeWindow must be positive");
        }
        if (thinBookQuantityThreshold < 0 || volatileSpreadThresholdTicks < 0) {
            throw new IllegalArgumentException("thresholds must be non-negative");
        }
        this.maxOutcomeWindow = maxOutcomeWindow;
        this.thinBookQuantityThreshold = thinBookQuantityThreshold;
        this.volatileSpreadThresholdTicks = volatileSpreadThresholdTicks;
    }
}
