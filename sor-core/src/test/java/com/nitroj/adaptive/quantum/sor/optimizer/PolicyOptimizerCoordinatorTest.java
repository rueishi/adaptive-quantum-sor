package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.compile.CompiledScoreConfig;
import com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler;
import com.nitroj.adaptive.quantum.sor.policy.lint.DefaultPolicyLint;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintConfig;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidateSet;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionConfig;
import com.nitroj.adaptive.quantum.sor.policy.validation.DefaultPolicyValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify policy optimizer coordinator behavior.
 *
 * <p>Role in system: covers P1-TC-016 cadence, snapshot metadata, optimizer
 * failure safety, and baseline-policy flow when optional models are disabled.</p>
 *
 * <p>Relationships: wires optimizer stubs, lint, compiler, validator, and
 * publisher together.</p>
 *
 * <p>Lifecycle: executed by Gradle as coordinator coverage.</p>
 *
 * <p>Design intent: ensure failed cycles do not replace active policy.</p>
 */
final class PolicyOptimizerCoordinatorTest {
    @Test
    void cycleRunsOnCadenceAndRecordsSnapshotMetadata() {
        final PolicyPublisher publisher = publisher();
        final PolicyOptimizerCoordinator coordinator = coordinator(publisher, new IsingCudaQStrategicOptimizerStub(2));

        final OptimizerCycleResult first = coordinator.runCycle(TestPolicyFixtures.input(), 1_000L);
        final OptimizerCycleResult second = coordinator.runCycle(TestPolicyFixtures.input(), 1_050L);

        assertTrue(first.cycleRan);
        assertTrue(first.published);
        assertEquals(7L, first.metadata.inputSnapshotId);
        assertFalse(second.cycleRan);
        assertEquals("cadence_not_elapsed", second.failureReason);
    }

    @Test
    void failedOptimizerDoesNotPublishAndOldPolicyRemainsActive() {
        final PolicyPublisher publisher = publisher();
        final PolicyOptimizerCoordinator good = coordinator(publisher, new IsingCudaQStrategicOptimizerStub(2));
        good.runCycle(TestPolicyFixtures.input(), 1_000L);
        final var old = publisher.activePolicy();
        final PolicyOptimizerCoordinator bad = coordinator(publisher, new IsingCudaQStrategicOptimizerStub(2, true));

        final OptimizerCycleResult result = bad.runCycle(TestPolicyFixtures.input(), 2_000L);

        assertTrue(result.cycleRan);
        assertFalse(result.published);
        assertSame(old, publisher.activePolicy());
        assertFalse(result.metadata.success);
    }

    @Test
    void optionalModelsDisabledStillProducesBaselinePolicy() {
        final var input = TestPolicyFixtures.input();
        input.modelSignals = null;
        final PolicyPublisher publisher = publisher();

        final OptimizerCycleResult result = coordinator(publisher, new IsingCudaQStrategicOptimizerStub(2)).runCycle(input, 1_000L);

        assertTrue(result.published);
        assertNotNull(publisher.activePolicy());
    }

    @Test
    void phase7CandidateSetPathBuildsDistinctOrdinalCandidatesWithoutPublishing() {
        final PolicyPublisher publisher = publisher();
        final PolicyOptimizerCoordinator coordinator = coordinator(publisher, new IsingCudaQStrategicOptimizerStub(2));

        final PolicyCandidateSet candidates = coordinator.buildCandidateSet(
                TestPolicyFixtures.input(),
                RobustSelectionConfig.defaults()
        );

        assertTrue(candidates.robustSelectionReady());
        assertTrue(candidates.size() >= 2);
        assertNull(publisher.activePolicy(), "candidate-set construction must not publish");
        for (int i = 0; i < candidates.candidates().size(); i++) {
            assertEquals(i, candidates.candidates().get(i).candidateId());
        }
        assertNotEquals(
                candidates.candidates().get(0).canonicalPolicyHash64(),
                candidates.candidates().get(1).canonicalPolicyHash64()
        );
    }

    @Test
    void configAndNullInputsFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> new OptimizerCadenceConfig(0));
        assertThrows(IllegalArgumentException.class, () -> coordinator(publisher(), new IsingCudaQStrategicOptimizerStub(2)).runCycle(null, 1L));
    }

    private static PolicyOptimizerCoordinator coordinator(final PolicyPublisher publisher, final StrategicVenueSubsetOptimizer strategic) {
        return new PolicyOptimizerCoordinator(new OptimizerCadenceConfig(100L), strategic, new CudaTacticalOptimizerStub(),
                new DefaultPolicyLint(PolicyLintConfig.defaults()), new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1)),
                new DefaultPolicyValidator(TestPolicyFixtures.VENUES), publisher);
    }

    private static PolicyPublisher publisher() {
        return new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
    }
}
