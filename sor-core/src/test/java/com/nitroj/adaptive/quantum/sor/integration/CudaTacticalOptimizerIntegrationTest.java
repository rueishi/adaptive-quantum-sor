package com.nitroj.adaptive.quantum.sor.integration;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.optnative.CudaFallbackPolicy;
import com.nitroj.sor.optnative.TacticalOptimizerNativeBridge;
import com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizer;
import com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.compile.CompiledScoreConfig;
import com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler;
import com.nitroj.adaptive.quantum.sor.policy.lint.DefaultPolicyLint;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintConfig;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGateResult;
import com.nitroj.adaptive.quantum.sor.policy.validation.DefaultPolicyValidator;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 2 CUDA tactical optimizer integration.
 *
 * <p>Role in system: covers P2-TC-007 by wiring CUDA/fallback tactical output
 * through candidate conversion, lint, compiler, validator, publisher, and
 * snapshot store.</p>
 *
 * <p>Relationships: consumes production optimizer, policy, validation,
 * publication, and governance classes instead of testing the wrapper in
 * isolation.</p>
 *
 * <p>Lifecycle: executed by Gradle as the dedicated Phase 2 integration test
 * for tactical optimizer publication safety.</p>
 *
 * <p>Design intent: prove CUDA backend replacement preserves the Phase 1 policy
 * pipeline and fail-safe publication behavior.</p>
 */
final class CudaTacticalOptimizerIntegrationTest {
    @Test
    void validCudaTacticalResultPublishesThroughPolicyPipeline() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizer(
                new TacticalOptimizerNativeBridge(),
                CudaFallbackPolicy.disabled(1_000_000_000L)
        ).optimize(strategic, input);
        final InMemoryPolicySnapshotStore store = new InMemoryPolicySnapshotStore();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), store);

        final SorPolicy policy = publish(input, strategic, tactical, publisher);

        assertSame(policy, publisher.activePolicy());
        assertSame(policy, store.load(policy.policyVersion));
    }

    @Test
    void invalidCudaTacticalResultIsRejectedBeforePublication() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.invalidOutputValues(),
                CudaFallbackPolicy.disabled(1_000_000_000L)
        ).optimize(strategic, input);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());

        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(candidate(input, strategic, tactical), input, strategic);

        assertTrue(lint.hasErrors());
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultPolicyCompiler(new CompiledScoreConfig(2, 2, 1))
                        .compile(candidate(input, strategic, tactical), strategic, tactical, lint));
        assertTrue(publisher.activePolicy() == null);
    }

    @Test
    void fallbackTacticalResultPublishesOnlyWhenFallbackEnabled() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.gpuUnavailable(),
                CudaFallbackPolicy.enabled(1_000_000L)
        );
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());

        final SorPolicy policy = publish(input, strategic, optimizer.optimize(strategic, input), publisher);

        assertNotNull(policy);
        assertSame(policy, publisher.activePolicy());
        assertFalse(optimizer.health().healthy());
        assertTrue(optimizer.lastMetadata().failureReason.contains("GPU_UNAVAILABLE"));
    }

    @Test
    void noFallbackLeavesActivePolicyUnchanged() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final SorPolicy prior = TestPolicyFixtures.policy();
        publisher.publish(prior, PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.timeout(),
                CudaFallbackPolicy.disabled(10L)
        );

        assertThrows(IllegalStateException.class, () -> optimizer.optimize(strategic, input));

        assertSame(prior, publisher.activePolicy());
        assertTrue(optimizer.lastMetadata().failureReason.contains("TIMEOUT"));
    }

    private static SorPolicy publish(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult strategic,
            final TacticalPolicyResult tactical,
            final PolicyPublisher publisher
    ) {
        final MutablePolicyCandidate candidate = candidate(input, strategic, tactical);
        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults()).lint(candidate, input, strategic);
        assertFalse(lint.hasErrors());
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 2, 1));
        final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lint);
        final PolicyValidationReport validation = new DefaultPolicyValidator(input.venueCount).validate(policy);
        assertTrue(validation.valid());
        final PublicationGateResult publication = publisher.publish(policy, validation, lint, compiler.lastDiff(), 10, 1L);
        assertTrue(publication.publishAllowed);
        return policy;
    }

    private static MutablePolicyCandidate candidate(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult strategic,
            final TacticalPolicyResult tactical
    ) {
        final MutablePolicyCandidate candidate =
                new MutablePolicyCandidate(input.instrumentCount, input.venueCount, input.regimeCount, input.urgencyCount);
        new CudaTacticalOptimizerStub().applyToCandidate(candidate, strategic, input, tactical);
        return candidate;
    }
}
