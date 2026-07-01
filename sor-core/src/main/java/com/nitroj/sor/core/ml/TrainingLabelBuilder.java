package com.nitroj.sor.core.ml;

import com.nitroj.sor.core.stats.ExecutionOutcomeStore;

/**
 * Responsibility: derive bounded supervised labels from execution outcomes.
 *
 * <p>Role in system: Phase 4 dataset export uses these labels for fill,
 * slippage, toxicity, and regime learning targets.</p>
 *
 * <p>Relationships: reads {@link ExecutionOutcomeStore} rows and writes
 * primitive label values into CSV training rows.</p>
 *
 * <p>Lifecycle: stateless helper reused by dataset exporters and tests.</p>
 *
 * <p>Design intent: labels are deliberately bounded so invalid simulator noise
 * cannot poison the Python training path.</p>
 */
public final class TrainingLabelBuilder {
    public TrainingLabels labels(final ExecutionOutcomeStore outcomes, final int outcomeIndex, final int regimeId) {
        if (outcomes == null) {
            throw new IllegalArgumentException("outcomes must not be null");
        }
        final int outcomeType = outcomes.outcomeType(outcomeIndex);
        final int fillBps = ExecutionOutcomeStore.isFill(outcomeType) ? 10_000 : 0;
        return new TrainingLabels(
                fillBps,
                clampBps(outcomes.slippageBps(outcomeIndex)),
                clampBps(outcomes.toxicityBps(outcomeIndex)),
                Math.max(0, regimeId)
        );
    }

    public static int clampBps(final int value) {
        return Math.max(0, Math.min(10_000, value));
    }

    public record TrainingLabels(
            int fillBps,
            int slippageBps,
            int toxicityBps,
            int regimeId
    ) {
    }
}
