package com.nitroj.sor.core.governance;

import com.nitroj.sor.core.optimizer.OptimizerRunMetadata;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.optimizer.ising.QuboObjectiveConfig;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify strategic optimizer audit lineage records.
 *
 * <p>Role in system: covers P3-TC-005 by proving accepted and rejected
 * strategic results carry optimizer run ID, objective config, input snapshot,
 * selected subset, and policy diff evidence.</p>
 *
 * <p>Relationships: exercises {@link StrategicOptimizerAudit} with Phase 3
 * QUBO and optimizer result objects.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: audit fields are copied so governance evidence remains
 * stable after optimizer objects leave the warm path.</p>
 */
final class StrategicOptimizerAuditTest {
    @Test
    void everyStrategicResultHasLineage() {
        final StrategicOptimizerAudit audit = StrategicOptimizerAudit.accepted(
                metadata(),
                objective(),
                input(),
                result(),
                diff()
        );

        assertEquals(42L, audit.optimizerRunId);
        assertEquals(3, audit.optimizerType);
        assertTrue(audit.accepted);
        assertEquals(7L, audit.inputSnapshotId);
        assertEquals(99L, audit.modelSignalVersion);
        assertEquals(5L, audit.currentPolicyVersion);
        assertEquals(0, audit.instrumentId);
        assertEquals(0, audit.regimeId);
        assertEquals(0, audit.urgencyId);
        assertArrayEquals(new int[]{-10, -20}, audit.objectiveLinearCoefficients);
        assertArrayEquals(new int[]{0, 30, 30, 0}, audit.objectivePairCoefficients);
        assertArrayEquals(new short[]{1}, audit.selectedVenueIds);
        assertEquals(4L, audit.strategicResultVersion);
        assertEquals(5L, audit.policyDiff.previousPolicyVersion);
        assertTrue(audit.selectedSubsetExplanation().contains("selectedVenueIds=[1]"));
    }

    @Test
    void reportIncludesAcceptedAndRejectedResults() {
        final StrategicOptimizerAudit rejected = StrategicOptimizerAudit.rejected(
                metadata(), objective(), input(), "backend unavailable");

        assertEquals(false, rejected.accepted);
        assertEquals("backend unavailable", rejected.rejectionReason);
        assertEquals(0L, rejected.strategicResultVersion);
        assertEquals("rejected: backend unavailable", rejected.selectedSubsetExplanation());
    }

    @Test
    void invalidAuditInputsRejected() {
        assertEquals("accepted result must not be null", assertThrows(IllegalArgumentException.class,
                () -> StrategicOptimizerAudit.accepted(metadata(), objective(), input(), null, diff())
        ).getMessage());
        assertEquals("rejectionReason must not be blank", assertThrows(IllegalArgumentException.class,
                () -> StrategicOptimizerAudit.rejected(metadata(), objective(), input(), " ")
        ).getMessage());
    }

    private static OptimizerRunMetadata metadata() {
        final OptimizerRunMetadata metadata = new OptimizerRunMetadata();
        metadata.optimizerRunId = 42L;
        metadata.optimizerType = 3;
        return metadata;
    }

    private static QuboObjectiveConfig objective() {
        return new QuboObjectiveConfig(0, 0, 0, 2, 1, 1, 100,
                new int[]{-10, -20},
                new int[]{0, 30, 30, 0});
    }

    private static PolicyOptimizationInput input() {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = 7L;
        input.modelSignalVersion = 99L;
        input.currentPolicyVersion = 5L;
        return input;
    }

    private static StrategicVenueSubsetResult result() {
        final StrategicVenueSubsetResult result = new StrategicVenueSubsetResult();
        result.version = 4L;
        result.selectedVenueIds = new short[]{1};
        return result;
    }

    private static PolicyDiff diff() {
        final PolicyDiff diff = new PolicyDiff();
        diff.previousPolicyVersion = 5L;
        diff.newPolicyVersion = 6L;
        diff.addedVenueCount = 1;
        return diff;
    }
}
