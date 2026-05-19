package com.nitroj.adaptive.quantum.sor.e2e;

import com.nitroj.adaptive.quantum.sor.api.SorHttpApiServer;
import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.audit.InMemoryRouteAuditWriter;
import com.nitroj.adaptive.quantum.sor.audit.RouteAuditEvent;
import com.nitroj.adaptive.quantum.sor.audit.RouteAuditEventBuffer;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.execution.StaticSorExecutioner;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.metrics.ComparisonRunner;
import com.nitroj.adaptive.quantum.sor.metrics.MetricsReporter;
import com.nitroj.adaptive.quantum.sor.metrics.MetricsSnapshot;
import com.nitroj.adaptive.quantum.sor.metrics.SorComparisonReport;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.nativebridge.CudaFallbackPolicy;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeBridge;
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
import com.nitroj.adaptive.quantum.sor.policy.validation.DefaultPolicyValidator;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.recovery.InitialPolicyBootstrap;
import com.nitroj.adaptive.quantum.sor.recovery.StartupRecoveryCoordinator;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioAssertions;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioRunner;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSummary;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: prove the SOR Adaptive Quantum SOR works as one local end-to-end system.
 *
 * <p>Role in system: this phase-neutral E2E suite complements task-card slice
 * integration tests by wiring recovery, active policy publication, execution,
 * audit, lifecycle, metrics/comparison, and HTTP control-plane behavior through
 * a coherent deterministic flow.</p>
 *
 * <p>Relationships: consumes production recovery, publisher, executioner,
 * state, audit, lifecycle, metrics, comparison, and API classes. Later Phase 2,
 * Phase 3, and ML phases can add methods here without renaming the suite.</p>
 *
 * <p>Lifecycle: executed by Gradle as a local-only JUnit test with no external
 * services, CUDA, CUDA-Q, real ML/RL, or notebook runtime.</p>
 *
 * <p>Design intent: keep a reusable confidence test that is broad enough to
 * catch broken system wiring while remaining deterministic and fast.</p>
 */
final class SorEndToEndTest {
    @Test
    void localEngineFlowRecoversPolicyRoutesOrderAuditsLifecycleAndComparesMetrics() {
        final SorConfig config = new SorConfig(1, 2, 1, 1, SorConfig.RuntimeMode.DEMO, true);
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(16, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final StartupRecoveryCoordinator recovery = new StartupRecoveryCoordinator(
                publisher,
                new InitialPolicyBootstrap(),
                lifecycleEvents
        );
        final EngineState state = engineState();
        final PolicyDrivenSorExecutioner adaptive = new PolicyDrivenSorExecutioner(
                publisher,
                state.market,
                state.sessions,
                state.risk
        );
        final OrderIntent order = order(101L, 600L);

        final RouteDecisionResult beforeRecovery = adaptive.route(order, new ChildOrderBuffer(4));
        assertEquals(OrderStatus.NO_ACTIVE_POLICY, beforeRecovery.status);
        assertEquals(0, beforeRecovery.childOrderCount);

        final StartupRecoveryCoordinator.StartupRecoveryResult recoveryResult =
                recovery.recover(config, Optional.empty(), 1L);
        assertTrue(recoveryResult.safeToRoute());
        assertNotNull(recoveryResult.activePolicy());
        assertEquals(recoveryResult.activePolicy(), publisher.activePolicy());

        final ChildOrderBuffer adaptiveChildren = new ChildOrderBuffer(4);
        final RouteDecisionResult adaptiveResult = adaptive.route(order, adaptiveChildren);
        assertEquals(OrderStatus.ACKED, adaptiveResult.status);
        assertEquals(1, adaptiveResult.childOrderCount);
        assertEquals(600L, adaptiveResult.routedQty);
        assertEquals(0L, adaptiveResult.residualQty);
        assertEquals(publisher.activePolicy().policyVersion, adaptiveChildren.get(0).policyVersion);
        assertEquals(publisher.activePolicy().policyHash64, adaptiveChildren.get(0).policyHash64);

        final RouteAuditEventBuffer auditBuffer = new RouteAuditEventBuffer(8, true);
        final InMemoryRouteAuditWriter auditWriter = new InMemoryRouteAuditWriter(auditBuffer);
        assertTrue(auditWriter.append(adaptiveResult.auditEvent));
        final RouteAuditEvent auditEvent = auditBuffer.snapshot().getFirst();
        assertEquals(order.parentOrderId, auditEvent.parentOrderId);
        assertEquals(publisher.activePolicy().policyVersion, auditEvent.policyVersion);
        assertEquals(publisher.activePolicy().policyHash64, auditEvent.policyHash64);

        lifecycleEvents.append(new LifecycleEvent(10L, 10L, 1, LifecycleEventType.SOR_DECISION,
                order.parentOrderId, "e2e route decision"));
        assertTrue(lifecycleEvents.snapshot().stream()
                .anyMatch(event -> event.eventType == LifecycleEventType.POLICY_PUBLISHED));
        assertTrue(lifecycleEvents.snapshot().stream()
                .anyMatch(event -> event.eventType == LifecycleEventType.SOR_DECISION));

        final StaticSorExecutioner staticSor = new StaticSorExecutioner(
                2,
                state.market,
                state.fees,
                state.sessions,
                state.risk
        );
        final RouteDecisionResult staticResult = staticSor.route(order(102L, 600L), new ChildOrderBuffer(4));
        final MetricsReporter metrics = new MetricsReporter();
        final MetricsSnapshot staticMetrics = metrics.snapshot(staticResult, 600L);
        final MetricsSnapshot adaptiveMetrics = metrics.snapshot(adaptiveResult, 600L);
        final SorComparisonReport report = new ComparisonRunner().compare(77L, staticMetrics, adaptiveMetrics);

        assertEquals(77L, report.scenarioId);
        assertTrue(report.staticSorRunId != report.adaptiveSorRunId);
        assertTrue(report.staticCompletionRateBps > 0);
        assertTrue(report.adaptiveCompletionRateBps > 0);
    }

    @Test
    void httpControlPlaneFlowSubmitsOrderReadsStatusStatsPolicyAndEvents() throws Exception {
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(16, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final StartupRecoveryCoordinator recovery = new StartupRecoveryCoordinator(
                publisher,
                new InitialPolicyBootstrap(),
                lifecycleEvents
        );
        final StartupRecoveryCoordinator.StartupRecoveryResult recoveryResult =
                recovery.recover(new SorConfig(1, 2, 1, 1, SorConfig.RuntimeMode.DEMO, true), Optional.empty(), 1L);
        assertTrue(recoveryResult.safeToRoute());

        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(8);
        final SorHttpApiServer server = new SorHttpApiServer(0, queue, publisher, lifecycleEvents);
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final String base = "http://127.0.0.1:" + server.port();
            final HttpResponse<String> order = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"instrumentId\":0,\"side\":1,\"quantity\":250,\"urgencyId\":0}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> status = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/orders/1")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> stats = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/stats/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> policy = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/policy/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> events = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/events/stream")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, order.statusCode());
            assertTrue(order.body().contains("\"parentOrderId\":1"));
            assertEquals(1, queue.size());
            assertEquals(200, status.statusCode());
            assertTrue(status.body().contains("\"remainingQty\":250"));
            assertEquals(200, stats.statusCode());
            assertTrue(stats.body().contains("\"policyVersion\":1"));
            assertTrue(stats.body().contains("\"venueCount\":2"));
            assertEquals(200, policy.statusCode());
            assertTrue(policy.body().contains("\"policyVersion\":1"));
            assertEquals(200, events.statusCode());
            assertTrue(events.body().contains("startup bootstrapped baseline policy"));
            assertTrue(events.body().contains("order accepted 1"));
        } finally {
            server.stop();
        }
    }

    @Test
    void cudaTacticalOptimizerFlowPublishesPolicyAndRoutesOrder() {
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(16, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final EngineState state = engineState(3);
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizer(
                new TacticalOptimizerNativeBridge(),
                CudaFallbackPolicy.disabled(1_000_000_000L),
                lifecycleEvents
        ).optimize(strategic, input);

        final SorPolicy policy = publishCudaPolicy(input, strategic, tactical, publisher, 2);
        final ChildOrderBuffer output = new ChildOrderBuffer(4);
        final RouteDecisionResult route = new PolicyDrivenSorExecutioner(
                publisher,
                state.market,
                state.sessions,
                state.risk
        ).route(order(201L, 500L), output);
        final MetricsSnapshot metrics = new MetricsReporter().snapshot(route, 500L);

        assertSame(policy, publisher.activePolicy());
        assertEquals(OrderStatus.ACKED, route.status);
        assertTrue(output.size() > 0);
        assertEquals(policy.policyVersion, output.get(0).policyVersion);
        assertTrue(metrics.completionRateBps > 0);
    }

    @Test
    void cudaFallbackFlowKeepsSystemSafeAndObservable() {
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(16, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final EngineState state = engineState(3);
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.gpuUnavailable(),
                CudaFallbackPolicy.enabled(1_000_000L),
                lifecycleEvents
        );

        final TacticalPolicyResult tactical = optimizer.optimize(strategic, input);
        final SorPolicy policy = publishCudaPolicy(input, strategic, tactical, publisher, 3);
        final RouteDecisionResult route = new PolicyDrivenSorExecutioner(
                publisher,
                state.market,
                state.sessions,
                state.risk
        ).route(order(202L, 500L), new ChildOrderBuffer(4));

        assertSame(policy, publisher.activePolicy());
        assertFalse(optimizer.health().healthy());
        assertEquals(OrderStatus.ACKED, route.status);
        assertTrue(lifecycleEvents.snapshot().stream()
                .anyMatch(event -> event.message.contains("CUDA fallback used")));
    }

    @Test
    void cudaNoFallbackFailureLeavesPriorActivePolicyUnchanged() {
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final SorPolicy prior = TestPolicyFixtures.policy();
        publisher.publish(prior, PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.timeout(),
                CudaFallbackPolicy.disabled(10L)
        );

        assertThrows(IllegalStateException.class, () -> optimizer.optimize(strategic, input));

        assertSame(prior, publisher.activePolicy());
        assertTrue(optimizer.lastMetadata().failureReason.contains("TIMEOUT"));
    }

    @Test
    void replayableScenarioProducesEquivalentSummary() {
        final ScenarioSpec spec = scenarioSpec("e2e-replay", 181L);
        final ScenarioRunner runner = new ScenarioRunner();

        final ScenarioSummary first = runner.run(spec);
        final ScenarioSummary second = runner.run(spec);

        ScenarioAssertions.assertReplayEquivalent(first, second);
        assertTrue(first.outcomeCount() > 0);
    }

    @Test
    void scenarioLiquidityDisappearanceRoutesSafely() {
        final EngineState state = engineState(2);
        state.market.updateTopOfBook(0, 0, 100L, 101L, 0L, 0L);
        state.market.updateTopOfBook(0, 1, 100L, 102L, 0L, 0L);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(TestPolicyFixtures.policy(), PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);

        final RouteDecisionResult result = new PolicyDrivenSorExecutioner(
                publisher,
                state.market,
                state.sessions,
                state.risk
        ).route(order(301L, 500L), new ChildOrderBuffer(4));

        assertEquals(OrderStatus.NO_LIQUIDITY, result.status);
        assertEquals(500L, result.residualQty);
    }

    @Test
    void liveScenarioPurgeAndRepopulateIsAudited() throws Exception {
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(32, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final SorHttpApiServer server = new SorHttpApiServer(0, new ParentOrderIntentQueue(16), publisher, lifecycleEvents,
                new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true));
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final String base = "http://127.0.0.1:" + server.port();
            final HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/scenario/run"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"scenarioId\":\"live-purge\",\"seed\":5,\"ticks\":2,\"resetMode\":\"PURGE_AND_REPOPULATE\","
                                    + "\"simulatorGeneratedOrders\":true}"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"success\":true"));
            assertTrue(response.body().contains("PURGE_AND_REPOPULATE"));
            assertTrue(lifecycleEvents.snapshot().stream().anyMatch(event -> event.message.contains("scenario reset live-purge")));
            assertTrue(lifecycleEvents.snapshot().stream().anyMatch(event -> event.message.contains("scenario run live-purge")));
        } finally {
            server.stop();
        }
    }

    @Test
    void liveScenarioKeepPolicyPurgeStatsPreservesPolicyHash() throws Exception {
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(32, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(TestPolicyFixtures.policy(), PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);
        final long policyHash = publisher.activePolicy().policyHash64;
        final SorHttpApiServer server = new SorHttpApiServer(0, new ParentOrderIntentQueue(16), publisher, lifecycleEvents,
                new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true));
        server.start();
        try {
            final HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/scenario/reset"))
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"scenarioId\":\"live-keep\",\"seed\":5,\"ticks\":2,\"resetMode\":\"KEEP_POLICY_PURGE_STATS\""))
                            .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(policyHash, publisher.activePolicy().policyHash64);
            assertTrue(response.body().contains("policySnapshotStore"));
        } finally {
            server.stop();
        }
    }

    private static EngineState engineState() {
        return engineState(2);
    }

    private static EngineState engineState(final int venueCount) {
        final MarketBookState market = new MarketBookState(1, venueCount);
        for (int venueId = 0; venueId < venueCount; venueId++) {
            market.updateTopOfBook(0, venueId, 100L, 101L + venueId, 1_000L, 1_000L);
        }

        final VenueSessionState sessions = new VenueSessionState(venueCount);
        for (int venueId = 0; venueId < venueCount; venueId++) {
            sessions.setStatus(venueId, VenueStatus.OPEN);
        }

        final RiskLimitSnapshot risk = new RiskLimitSnapshot(1, venueCount);
        risk.setMaxChildQty(0, 1_000L);
        for (int venueId = 0; venueId < venueCount; venueId++) {
            risk.setVenueLimits(0, venueId, 1_000_000L, 2_500);
        }

        final FeeScheduleSnapshot fees = new FeeScheduleSnapshot(1, venueCount);
        for (int venueId = 0; venueId < venueCount; venueId++) {
            fees.setFees(0, venueId, 0, 1 + venueId);
        }
        return new EngineState(market, sessions, risk, fees);
    }

    private static OrderIntent order(final long parentOrderId, final long quantity) {
        return new OrderIntent(parentOrderId, 0, Side.BUY, quantity, 0, 1L);
    }

    private static ScenarioSpec scenarioSpec(final String scenarioId, final long seed) {
        return new ScenarioSpec(scenarioId, seed, 6, new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true),
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 1, 0),
                        new ScenarioWindow(2, 3, 1),
                        new ScenarioWindow(4, 5, 2)
                },
                true,
                ScenarioSimulatorConfig.defaults());
    }

    private static SorPolicy publishCudaPolicy(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult strategic,
            final TacticalPolicyResult tactical,
            final PolicyPublisher publisher,
            final long nowNanos
    ) {
        final MutablePolicyCandidate candidate =
                new MutablePolicyCandidate(input.instrumentCount, input.venueCount, input.regimeCount, input.urgencyCount);
        new CudaTacticalOptimizerStub().applyToCandidate(candidate, strategic, input, tactical);
        final PolicyLintReport lint = new DefaultPolicyLint(PolicyLintConfig.defaults()).lint(candidate, input, strategic);
        assertFalse(lint.hasErrors());
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 2, 1));
        final SorPolicy policy = compiler.compile(candidate, strategic, tactical, lint);
        final PolicyValidationReport validation = new DefaultPolicyValidator(input.venueCount).validate(policy);
        assertTrue(validation.valid());
        assertTrue(publisher.publish(policy, validation, lint, compiler.lastDiff(), 10, nowNanos).publishAllowed);
        return policy;
    }

    /**
     * Responsibility: hold deterministic state shared by E2E engine routing.
     *
     * <p>Role in system: keeps market/session/risk/fee setup together so the
     * E2E methods read as system behavior rather than fixture plumbing.</p>
     *
     * <p>Relationships: consumed by static and policy-driven executioners.</p>
     *
     * <p>Lifecycle: immutable test value created per E2E method.</p>
     *
     * <p>Design intent: preserve deterministic local state for Phase 1 and
     * future phase extensions.</p>
     */
    private record EngineState(
            MarketBookState market,
            VenueSessionState sessions,
            RiskLimitSnapshot risk,
            FeeScheduleSnapshot fees
    ) {
    }
}
