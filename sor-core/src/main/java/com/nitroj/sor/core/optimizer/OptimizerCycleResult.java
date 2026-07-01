package com.nitroj.sor.core.optimizer;

import com.nitroj.sor.core.policy.publication.PublicationGateResult;

/**
 * Responsibility: summarize one optimizer coordinator cycle.
 *
 * <p>Role in system: callers and tests use this result to know whether a cycle
 * ran, whether publication succeeded, and which snapshot metadata was recorded.</p>
 *
 * <p>Relationships: produced by {@link PolicyOptimizerCoordinator} and carries
 * {@link OptimizerRunMetadata} plus publication gate output.</p>
 *
 * <p>Lifecycle: immutable after creation.</p>
 *
 * <p>Design intent: a small result object keeps coordinator error handling
 * observable without throwing for expected optimizer failures.</p>
 */
public final class OptimizerCycleResult {
    public final boolean cycleRan;
    public final boolean published;
    public final OptimizerRunMetadata metadata;
    public final PublicationGateResult publicationResult;
    public final String failureReason;

    public OptimizerCycleResult(
            final boolean cycleRan,
            final boolean published,
            final OptimizerRunMetadata metadata,
            final PublicationGateResult publicationResult,
            final String failureReason
    ) {
        this.cycleRan = cycleRan;
        this.published = published;
        this.metadata = metadata;
        this.publicationResult = publicationResult;
        this.failureReason = failureReason;
    }
}
