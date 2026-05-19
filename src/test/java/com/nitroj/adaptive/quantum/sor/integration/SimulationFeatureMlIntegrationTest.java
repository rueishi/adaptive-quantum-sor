package com.nitroj.adaptive.quantum.sor.integration;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.feature.FeatureAggregator;
import com.nitroj.adaptive.quantum.sor.feature.RollingWindowConfig;
import com.nitroj.adaptive.quantum.sor.ml.MlSignalModelStub;
import com.nitroj.adaptive.quantum.sor.ml.ModelSignalVersion;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.ModelSignalState;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
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
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.execution.StaticSorExecutioner;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.metrics.ComparisonRunner;
import com.nitroj.adaptive.quantum.sor.metrics.MetricsReporter;
import com.nitroj.adaptive.quantum.sor.metrics.SorComparisonReport;
import com.nitroj.adaptive.quantum.sor.sim.SyntheticScenarioGenerator;
import com.nitroj.adaptive.quantum.sor.sim.VenueBehaviorSimulator;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.FillQualityStats;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import com.nitroj.adaptive.quantum.sor.stats.SlippageStats;
import com.nitroj.adaptive.quantum.sor.stats.ToxicityStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueHealthStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueLatencyStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueStatsState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the implemented Phase 1 simulation-to-model slice.
 *
 * <p>Role in system: this integration test covers the cross-component path
 * added by P1-TC-007 through P1-TC-015: deterministic scenario state, simulated
 * venue outcomes, feature aggregation, regime detection, ML-style signal
 * generation, optimizer stubs, lint, compilation, validation, publication, and
 * snapshot storage.</p>
 *
 * <p>Relationships: intentionally wires simulators, state containers, stats,
 * {@link FeatureAggregator}, and {@link MlSignalModelStub} instead of testing
 * any one class in isolation.</p>
 *
 * <p>Lifecycle: executed by Gradle's standard test task as the current Phase 1
 * integration coverage until dedicated integration-test source sets are added
 * by the later CI test profile card.</p>
 *
 * <p>Design intent: prove the task-card-owned structures interoperate without
     * requiring the later policy-driven executioner or API cards.</p>
 */
final class SimulationFeatureMlIntegrationTest {
    @Test
    void simulatorOutcomesAggregateIntoModelSignals() {
        final SorConfig config = new SorConfig(1, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);
        final MarketBookState marketBookState = new MarketBookState(config.instrumentCount(), config.venueCount());
        final VenueSessionState venueSessionState = new VenueSessionState(config.venueCount());
        final VenueThrottleState venueThrottleState = new VenueThrottleState(config.venueCount());
        final MarketSessionState marketSessionState = new MarketSessionState(config.instrumentCount());

        new SyntheticScenarioGenerator(config, 17L).populateBaseline(
                marketBookState,
                venueSessionState,
                venueThrottleState,
                marketSessionState
        );

        final ChildOrderBuffer childOrders = new ChildOrderBuffer(2);
        childOrders.add(1L, 100L, 0, 0, Side.BUY, 1_000L, 1L, 99L);
        childOrders.add(2L, 100L, 0, 3, Side.BUY, 1_000L, 1L, 99L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(16);
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(2);
        final ChildOrderState childOrderState = new ChildOrderState(2);

        new VenueBehaviorSimulator(config, 4L).process(childOrders, outcomes, outstanding, childOrderState);

        final VenueStatsState venueStats = new VenueStatsState(config.instrumentCount(), config.venueCount(), config.regimeCount());
        final VenueLatencyStats latencyStats = new VenueLatencyStats(config.venueCount());
        final FillQualityStats fillQualityStats = new FillQualityStats(config.venueCount());
        final ToxicityStats toxicityStats = new ToxicityStats(config.venueCount());
        final SlippageStats slippageStats = new SlippageStats(config.venueCount());
        final VenueHealthStats healthStats = new VenueHealthStats(config.venueCount());
        final RegimeState regimeState = new RegimeState(config.instrumentCount());

        new FeatureAggregator(
                config.instrumentCount(),
                config.venueCount(),
                config.regimeCount(),
                new RollingWindowConfig(16, 100, 10)
        ).aggregate(
                marketBookState,
                outcomes,
                venueStats,
                latencyStats,
                fillQualityStats,
                toxicityStats,
                slippageStats,
                healthStats,
                regimeState
        );

        final ModelSignalState signals = new MlSignalModelStub(
                config.instrumentCount(),
                config.venueCount(),
                config.regimeCount()
        ).generate(venueStats, fillQualityStats, toxicityStats, slippageStats, regimeState);
        final com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput input = new com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput();
        input.inputSnapshotId = 77L;
        input.createdAtNanos = 200L;
        input.instrumentCount = config.instrumentCount();
        input.venueCount = config.venueCount();
        input.regimeCount = config.regimeCount();
        input.urgencyCount = config.urgencyCount();
        input.venueStats = venueStats;
        input.modelSignals = signals;
        input.riskLimits = new com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot(config.instrumentCount(), config.venueCount());
        input.riskLimits.setMaxChildQty(0, 5_000L);
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            input.riskLimits.setVenueLimits(0, venueId, 1_000_000L, 2_500);
        }
        input.orderTypeCapabilities = com.nitroj.adaptive.quantum.sor.metadata.OrderTypeCapabilityMatrix.simulated(config.venueCount());

        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizerStub tacticalOptimizer = new CudaTacticalOptimizerStub();
        final TacticalPolicyResult tactical = tacticalOptimizer.optimize(strategic, input);
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(
                config.instrumentCount(),
                config.venueCount(),
                config.regimeCount(),
                config.urgencyCount()
        );
        tacticalOptimizer.applyToCandidate(candidate, strategic, input, tactical);
        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults()).lint(candidate, input, strategic);
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1));
        final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lint);
        final PolicyValidationReport validation = new DefaultPolicyValidator(config.venueCount()).validate(policy);
        final InMemoryPolicySnapshotStore snapshotStore = new InMemoryPolicySnapshotStore();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), snapshotStore);
        final PublicationGateResult publish = publisher.publish(policy, validation, lint, compiler.lastDiff(), 10, 1_000L);
        final com.nitroj.adaptive.quantum.sor.model.OrderIntent order = new com.nitroj.adaptive.quantum.sor.model.OrderIntent(10L, 0, Side.BUY, 200L, 0, 1L);
        final com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer adaptiveChildren = new com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer(4);
        final RouteDecisionResult adaptiveRoute = new PolicyDrivenSorExecutioner(
                publisher,
                marketBookState,
                venueSessionState,
                input.riskLimits
        ).route(order, adaptiveChildren);
        final FeeScheduleSnapshot fees = new FeeScheduleSnapshot(config.instrumentCount(), config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            fees.setFees(0, venueId, 0, venueId);
        }
        final RouteDecisionResult staticRoute = new StaticSorExecutioner(
                config.venueCount(),
                marketBookState,
                fees,
                venueSessionState,
                input.riskLimits
        ).route(order, new com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer(4));
        final MetricsReporter metricsReporter = new MetricsReporter();
        final SorComparisonReport comparison = new ComparisonRunner().compare(
                99L,
                metricsReporter.snapshot(staticRoute, order.quantity),
                metricsReporter.snapshot(adaptiveRoute, order.quantity)
        );

        assertTrue(outcomes.size() >= childOrders.size(), "venue simulator produced outcomes for child orders");
        assertTrue(venueStats.sequence() > 0, "feature aggregation updated consolidated venue stats");
        assertEquals(ModelSignalVersion.PHASE1_STUB_V1, signals.modelVersion());
        assertTrue(signals.venueScoreBps(0, 0, 0) >= 0);
        assertTrue(signals.venueScoreBps(0, 0, 0) <= 10_000);
        assertNotEquals(5_000, signals.venueScoreBps(0, 0, 0), "non-neutral outcomes influence model signals");
        assertTrue(marketSessionState.isOpen(0), "scenario generator opened the market session");
        assertTrue(venueSessionState.isAvailable(0), "scenario generator opened the routed venue");
        assertTrue(venueThrottleState.maxOrderRatePerSecond(0) > 0, "scenario generator populated throttles");
        assertFalse(lint.hasErrors(), "optimizer candidate passes lint");
        assertTrue(validation.valid(), "compiled policy passes validation");
        assertTrue(publish.publishAllowed, "valid policy publishes");
        assertSame(policy, publisher.activePolicy(), "publisher exposes active policy");
        assertSame(policy, snapshotStore.load(policy.policyVersion), "published policy is snapshotted");
        assertTrue(adaptiveRoute.childOrderCount > 0, "adaptive SOR routes with active policy");
        assertTrue(staticRoute.childOrderCount > 0, "static SOR routes same scenario");
        assertEquals(99L, comparison.scenarioId, "comparison report emitted for scenario");
    }
}
