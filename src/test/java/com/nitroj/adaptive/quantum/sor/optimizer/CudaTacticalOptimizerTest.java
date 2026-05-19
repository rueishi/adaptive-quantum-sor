package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.nativebridge.CudaFallbackPolicy;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeBridge;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the deterministic CUDA tactical optimizer MVP.
 *
 * <p>Role in system: covers P2-TC-003 valid CUDA output, invalid output lint
 * rejection, and deterministic same-input behavior.</p>
 *
 * <p>Relationships: wires native bridge output through the Phase 1 candidate,
 * lint, compiler, validation, and publisher pipeline.</p>
 *
 * <p>Lifecycle: executed by Gradle without requiring CUDA hardware.</p>
 *
 * <p>Design intent: prove backend replacement preserves the policy pipeline
 * contract before hardware-specific optimization is introduced.</p>
 */
final class CudaTacticalOptimizerTest {
    @Test
    void validCudaOutputCompilesAndPublishesThroughExistingPipeline() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                new TacticalOptimizerNativeBridge(),
                CudaFallbackPolicy.disabled(1_000_000_000L)
        );

        final TacticalPolicyResult tactical = optimizer.optimize(strategic, input);
        final MutablePolicyCandidate candidate = candidate(input, strategic, tactical);
        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults()).lint(candidate, input, strategic);
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 2, 1));
        final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lint);
        final PolicyValidationReport validation = new DefaultPolicyValidator(input.venueCount).validate(policy);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final PublicationGateResult publication = publisher.publish(policy, validation, lint, compiler.lastDiff(), 10, 1L);

        assertTrue(lint.issues().isEmpty());
        assertTrue(validation.valid());
        assertTrue(publication.publishAllowed);
        assertSame(policy, publisher.activePolicy());
        assertTrue(optimizer.health().healthy());
        assertTrue(optimizer.lastMetadata().success);
    }

    @Test
    void invalidCudaOutputIsRejectedByPolicyLint() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.invalidOutputValues(),
                CudaFallbackPolicy.disabled(1_000_000_000L)
        ).optimize(strategic, input);

        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(candidate(input, strategic, tactical), input, strategic);

        assertTrue(lint.hasErrors());
    }

    @Test
    void deterministicSameInputProducesSameOutput() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaFallbackPolicy policy = CudaFallbackPolicy.disabled(1_000_000_000L);

        final TacticalPolicyResult first = new CudaTacticalOptimizer(new TacticalOptimizerNativeBridge(), policy)
                .optimize(strategic, input);
        final TacticalPolicyResult second = new CudaTacticalOptimizer(new TacticalOptimizerNativeBridge(), policy)
                .optimize(strategic, input);

        assertArrayEquals(first.venueWeightBps, second.venueWeightBps);
        assertArrayEquals(first.fillProbabilityBps, second.fillProbabilityBps);
        assertArrayEquals(first.maxChildQty, second.maxChildQty);
        assertFalse(first == second);
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
