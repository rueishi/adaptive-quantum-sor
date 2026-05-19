package com.nitroj.adaptive.quantum.sor.optimizer;

/**
 * Responsibility: summarize model contribution to optimizer output.
 *
 * <p>Role in system: audit, explanation APIs, and later notebooks can use this
 * compact trace to show how model signals affected a policy candidate.</p>
 *
 * <p>Relationships: emitted by ML/optimizer stubs and linked to optimizer run
 * metadata.</p>
 *
 * <p>Lifecycle: created during a model or optimizer run and retained for
 * governance/explanation flows.</p>
 *
 * <p>Design intent: this Phase 1 structure mirrors the spec with primitive
 * fields and avoids tying explanations to a specific ML implementation.</p>
 */
public final class ModelContributionTrace {
    public long policyVersion;
    public long optimizerRunId;
    public int modelId;
    public int contributionType;
    public int affectedEntryCount;
    public int estimatedImpactBps;
}
