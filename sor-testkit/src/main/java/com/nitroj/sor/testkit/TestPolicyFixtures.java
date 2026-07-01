package com.nitroj.sor.testkit;

import com.nitroj.sor.core.metadata.OrderTypeCapabilityMatrix;
import com.nitroj.sor.core.ml.MlSignalModelStub;
import com.nitroj.sor.core.model.ModelSignalState;
import com.nitroj.sor.core.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.sor.core.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.optimizer.TacticalPolicyResult;
import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.compile.CompiledScoreConfig;
import com.nitroj.sor.core.policy.compile.DefaultPolicyCompiler;
import com.nitroj.sor.core.policy.lint.DefaultPolicyLint;
import com.nitroj.sor.core.policy.lint.PolicyLintConfig;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.stats.FillQualityStats;
import com.nitroj.sor.core.stats.RegimeState;
import com.nitroj.sor.core.stats.SlippageStats;
import com.nitroj.sor.core.stats.ToxicityStats;
import com.nitroj.sor.core.stats.VenueStatsState;

/**
 * Responsibility: provide deterministic policy fixtures for task-card tests.
 *
 * <p>Role in system: tests for optimizers, lint, validation, compilation, and
 * publication need the same small but valid candidate and policy graph.</p>
 *
 * <p>Relationships: builds objects from production policy, optimizer, stats,
 * metadata, and risk packages.</p>
 *
 * <p>Lifecycle: static test helper used only by JUnit tests.</p>
 *
 * <p>Design intent: keep repetitive fixture setup out of individual tests so
 * each test can focus on the behavior it owns.</p>
 */
public final class TestPolicyFixtures {
    public static final int INSTRUMENTS = 1;
    public static final int VENUES = 3;
    public static final int REGIMES = 3;
    public static final int URGENCIES = 2;

    private TestPolicyFixtures() {
    }

    /** Builds a policy optimization input with bounded stats and metadata. */
    public static PolicyOptimizationInput input() {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = 7L;
        input.createdAtNanos = 100L;
        input.instrumentCount = INSTRUMENTS;
        input.venueCount = VENUES;
        input.regimeCount = REGIMES;
        input.urgencyCount = URGENCIES;
        input.venueStats = new VenueStatsState(INSTRUMENTS, VENUES, REGIMES);
        for (int venueId = 0; venueId < VENUES; venueId++) {
            for (int regimeId = 0; regimeId < REGIMES; regimeId++) {
                input.venueStats.update(0, venueId, regimeId, 1_000_000 + venueId, 9_000 - venueId * 1_000,
                        venueId * 100, venueId * 50, 0, venueId * 10);
            }
        }
        final FillQualityStats fill = new FillQualityStats(VENUES);
        final ToxicityStats toxicity = new ToxicityStats(VENUES);
        final SlippageStats slippage = new SlippageStats(VENUES);
        final RegimeState regime = new RegimeState(INSTRUMENTS);
        input.modelSignals = new MlSignalModelStub(INSTRUMENTS, VENUES, REGIMES).generate(input.venueStats, fill, toxicity, slippage, regime);
        input.orderTypeCapabilities = OrderTypeCapabilityMatrix.simulated(VENUES);
        input.riskLimits = new RiskLimitSnapshot(INSTRUMENTS, VENUES);
        input.riskLimits.setMaxChildQty(0, 5_000L);
        for (int venueId = 0; venueId < VENUES; venueId++) {
            input.riskLimits.setVenueLimits(0, venueId, 1_000_000L, 2_500);
        }
        return input;
    }

    /** Builds a valid candidate by running the Phase 1 optimizer stubs. */
    public static CandidateBundle candidateBundle() {
        final PolicyOptimizationInput input = input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizerStub().optimize(strategic, input);
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(INSTRUMENTS, VENUES, REGIMES, URGENCIES);
        new CudaTacticalOptimizerStub().applyToCandidate(candidate, strategic, input, tactical);
        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults()).lint(candidate, input, strategic);
        return new CandidateBundle(input, strategic, tactical, candidate, lint);
    }

    /** Compiles and returns a valid policy fixture. */
    public static SorPolicy policy() {
        final CandidateBundle bundle = candidateBundle();
        return new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1))
                .compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());
    }

    /**
     * Responsibility: group candidate-related fixtures.
     *
     * <p>Role in system: keeps test setup values together while preserving
     * readable named accessors.</p>
     *
     * <p>Relationships: returned by {@link #candidateBundle()}.</p>
     *
     * <p>Lifecycle: immutable test value object.</p>
     *
     * <p>Design intent: records reduce boilerplate in behavior-focused tests.</p>
     */
    public record CandidateBundle(
            PolicyOptimizationInput input,
            StrategicVenueSubsetResult strategic,
            TacticalPolicyResult tactical,
            MutablePolicyCandidate candidate,
            PolicyLintReport lint
    ) {
    }
}
