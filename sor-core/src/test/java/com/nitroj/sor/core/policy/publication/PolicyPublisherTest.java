package com.nitroj.sor.core.policy.publication;

import com.nitroj.sor.core.TestPolicyFixtures;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify publication gates, atomic publish, ledger, snapshots,
 * and rollback behavior.
 *
 * <p>Role in system: covers P1-TC-015 publication safety and basic rollback
 * after policy compilation/validation.</p>
 *
 * <p>Relationships: uses compiled policy fixtures, validation reports, lint
 * reports, {@link PublicationGate}, and {@link InMemoryPolicySnapshotStore}.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct unit coverage for publication.</p>
 *
 * <p>Design intent: publication tests ensure failed gates leave the old active
 * policy untouched.</p>
 */
final class PolicyPublisherTest {
    @Test
    void validPolicyPublishesAndWritesLedgerAndSnapshot() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        final InMemoryPolicySnapshotStore store = new InMemoryPolicySnapshotStore();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), store);

        final PublicationGateResult result = publisher.publish(policy, PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()), diff(1, 100), 10, 1_000L);

        assertTrue(result.publishAllowed);
        assertSame(policy, publisher.activePolicy());
        assertSame(policy, store.load(policy.policyVersion));
        assertEquals(1, publisher.ledger().size());
    }

    @Test
    void failedGateDoesNotPublishAndOldPolicyRemainsActive() {
        final SorPolicy first = TestPolicyFixtures.policy();
        final SorPolicy second = TestPolicyFixtures.policy();
        final PolicyPublisher publisher = new PolicyPublisher(new PublicationGate(100L, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
                new InMemoryPolicySnapshotStore());
        publisher.publish(first, PolicyValidationReport.validReport(), new PolicyLintReport(List.of()), diff(1, 100), 10, 1_000L);

        final PublicationGateResult result = publisher.publish(second, PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()), diff(1, 100), 10, 1_050L);

        assertFalse(result.publishAllowed);
        assertSame(first, publisher.activePolicy());
        assertEquals("publish_interval", result.failedGates[0]);
    }

    @Test
    void hashMismatchAndValidationFailureReject() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());

        final PublicationGateResult validation = publisher.publish(policy, new PolicyValidationReport(List.of("bad")),
                new PolicyLintReport(List.of()), diff(1, 100), 10, 1L);
        final PublicationGateResult hash = new PublicationGate(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)
                .evaluate(null, policyWithZeroHash(policy), PolicyValidationReport.validReport(),
                        new PolicyLintReport(List.of()), diff(1, 100), 1L, 0L, 10);

        assertFalse(validation.publishAllowed);
        assertEquals("validation_failed", validation.failedGates[0]);
        assertFalse(hash.publishAllowed);
        assertEquals("hash_mismatch", hash.failedGates[0]);
    }

    @Test
    void rollbackRestoresPriorPolicy() {
        final SorPolicy first = TestPolicyFixtures.policy();
        final SorPolicy second = TestPolicyFixtures.policy();
        final InMemoryPolicySnapshotStore store = new InMemoryPolicySnapshotStore();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), store);
        publisher.publish(first, PolicyValidationReport.validReport(), new PolicyLintReport(List.of()), diff(1, 100), 10, 1L);
        publisher.publish(second, PolicyValidationReport.validReport(), new PolicyLintReport(List.of()), diff(1, 100), 10, 2L);

        assertTrue(publisher.rollback(first.policyVersion));

        assertEquals(first.policyVersion, publisher.activePolicy().policyVersion);
        assertFalse(publisher.rollback(999L));
    }

    @Test
    void improvementAndThrashingLimitsRejectExcessiveChurn() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        final PublicationGate gate = new PublicationGate(0L, 5, 1, 50);

        assertEquals("expected_improvement", gate.evaluate(null, policy, PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()), diff(1, 40), 1L, 0L, 1).failedGates[0]);
        assertEquals("venue_churn", gate.evaluate(null, policy, PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()), diff(2, 40), 1L, 0L, 10).failedGates[0]);
        assertEquals("weight_churn", gate.evaluate(null, policy, PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()), diff(1, 100), 1L, 0L, 10).failedGates[0]);
        assertThrows(IllegalArgumentException.class, () -> new PublicationGate(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PolicyPublisher(null, new InMemoryPolicySnapshotStore()));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryPolicySnapshotStore().store(null));
    }

    private static PolicyDiff diff(final int addedVenueCount, final int maxWeightChangeBps) {
        final PolicyDiff diff = new PolicyDiff();
        diff.addedVenueCount = addedVenueCount;
        diff.maxWeightChangeBps = maxWeightChangeBps;
        return diff;
    }

    private static SorPolicy policyWithZeroHash(final SorPolicy policy) {
        return new SorPolicy(policy.policyVersion, policy.createdAtEpochNanos, policy.effectiveFromEpochNanos,
                0L, new byte[32], policy.isingResultVersion, policy.cudaTuningVersion, policy.optimizerType,
                policy.policyState, policy.hotRouteBook, policy.fullPolicyMatrix);
    }
}
