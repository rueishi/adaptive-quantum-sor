package com.nitroj.adaptive.quantum.sor.governance;

/**
 * Responsibility: record one policy publication or rejection decision.
 *
 * <p>Role in system: the ledger entry provides governance lineage for a policy
 * version, connecting optimizer output, policy hash, diff summary, and publish
 * outcome.</p>
 *
 * <p>Relationships: policy publishers and future snapshot stores create these
 * entries; audit and lifecycle layers read them for operator explanations.</p>
 *
 * <p>Lifecycle: appended once per policy decision and then treated as immutable
 * audit data.</p>
 *
 * <p>Design intent: fields are primitive/string references so the Phase 1
 * governance trail can exist before binary persistence is implemented.</p>
 */
public final class PolicyChangeLedgerEntry {
    public long ledgerEntryId;
    public long timestampNanos;
    public long previousPolicyVersion;
    public long newPolicyVersion;
    public long newPolicyHash64;
    public long optimizerRunId;
    public boolean published;
    public String reason;
    public PolicyDiff diff;
    public String robustObjective;
    public String robustObjectiveParameters;
    public String scenarioSetId;
    public long scenarioSetVersion;
    public int scenarioCount;
    public int candidateCount;
    public String scoreMatrixHandle;
    public String adequacyStatus;
    public String[] adequacyMissingCategories;
}
