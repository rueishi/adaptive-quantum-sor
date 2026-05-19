package com.nitroj.adaptive.quantum.sor.governance;

/**
 * Responsibility: summarize differences between two policy versions.
 *
 * <p>Role in system: governance, lifecycle logging, and API views use this
 * compact summary to explain policy churn without diffing full matrices during
 * operator-facing reads.</p>
 *
 * <p>Relationships: produced by policy compilation or governance code and
 * linked from {@link PolicyChangeLedgerEntry}.</p>
 *
 * <p>Lifecycle: created when a candidate policy is compared with the prior
 * active policy and then retained for audit or display.</p>
 *
 * <p>Design intent: public primitive fields keep the Phase 1 representation
 * simple and serializable.</p>
 */
public final class PolicyDiff {
    public long previousPolicyVersion;
    public long newPolicyVersion;
    public int changedRouteListCount;
    public int changedWeightCount;
    public int changedPenaltyCount;
    public int addedVenueCount;
    public int removedVenueCount;
    public int maxWeightChangeBps;
}
