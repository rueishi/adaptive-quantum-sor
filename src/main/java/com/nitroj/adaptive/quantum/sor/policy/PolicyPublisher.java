package com.nitroj.adaptive.quantum.sor.policy;

import com.nitroj.adaptive.quantum.sor.governance.PolicyChangeLedgerEntry;
import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.governance.PolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGateResult;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Responsibility: atomically publish and roll back immutable SOR policies.
 *
 * <p>Role in system: this class owns the active policy reference used by future
 * execution and API layers.</p>
 *
 * <p>Relationships: consumes publication gate decisions and writes snapshots
 * through {@link PolicySnapshotStore}; records ledger entries for governance.</p>
 *
 * <p>Lifecycle: created at startup, then reused for every publication attempt.
 * Route execution will read {@link #activePolicy()} without mutating it.</p>
 *
 * <p>Design intent: isolate atomic reference swapping from compiler and gate
 * logic so publication safety is easy to reason about and test.</p>
 */
public final class PolicyPublisher {
    private final AtomicReference<SorPolicy> activePolicy = new AtomicReference<>();
    private final PublicationGate gate;
    private final PolicySnapshotStore snapshotStore;
    private final List<PolicyChangeLedgerEntry> ledger = new ArrayList<>();
    private long lastPublishNanos;

    public PolicyPublisher(final PublicationGate gate, final PolicySnapshotStore snapshotStore) {
        if (gate == null || snapshotStore == null) {
            throw new IllegalArgumentException("gate and snapshotStore must not be null");
        }
        this.gate = gate;
        this.snapshotStore = snapshotStore;
    }

    public SorPolicy activePolicy() {
        return activePolicy.get();
    }

    /**
     * Publishes a policy only when validation, lint, hash, cadence, and churn
     * gates allow it.
     */
    public PublicationGateResult publish(
            final SorPolicy candidate,
            final PolicyValidationReport validationReport,
            final PolicyLintReport lintReport,
            final PolicyDiff diff,
            final int expectedImprovementBps,
            final long nowNanos
    ) {
        final PublicationGateResult result = gate.evaluate(
                activePolicy.get(),
                candidate,
                validationReport,
                lintReport,
                diff,
                nowNanos,
                lastPublishNanos,
                expectedImprovementBps
        );
        if (!result.publishAllowed) {
            return result;
        }
        activePolicy.set(candidate);
        lastPublishNanos = nowNanos;
        snapshotStore.store(candidate);
        ledger.add(ledgerEntry(candidate, diff, true, "PUBLISHED"));
        return result;
    }

    /**
     * Restores a previously stored policy version.
     */
    public boolean rollback(final long policyVersion) {
        final SorPolicy snapshot = snapshotStore.load(policyVersion);
        if (snapshot == null) {
            return false;
        }
        activePolicy.set(snapshot);
        ledger.add(ledgerEntry(snapshot, null, true, "ROLLBACK"));
        return true;
    }

    public List<PolicyChangeLedgerEntry> ledger() {
        return List.copyOf(ledger);
    }

    private static PolicyChangeLedgerEntry ledgerEntry(
            final SorPolicy policy,
            final PolicyDiff diff,
            final boolean published,
            final String reason
    ) {
        final PolicyChangeLedgerEntry entry = new PolicyChangeLedgerEntry();
        entry.ledgerEntryId = policy.policyVersion;
        entry.timestampNanos = policy.createdAtEpochNanos;
        entry.newPolicyVersion = policy.policyVersion;
        entry.newPolicyHash64 = policy.policyHash64;
        entry.published = published;
        entry.reason = reason;
        entry.diff = diff;
        return entry;
    }
}
