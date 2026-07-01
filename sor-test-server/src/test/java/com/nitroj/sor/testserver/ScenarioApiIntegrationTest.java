package com.nitroj.sor.testserver;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.core.execution.PolicyDrivenSorExecutioner;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;
import com.nitroj.sor.core.metadata.InstrumentMetadata;
import com.nitroj.sor.core.metadata.VenueMetadata;
import com.nitroj.sor.core.model.ChildOrderBuffer;
import com.nitroj.sor.core.model.ParentOrderIntentQueue;
import com.nitroj.sor.core.model.VenueStatus;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.publication.PublicationGate;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;
import com.nitroj.sor.core.recovery.InitialPolicyBootstrap;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.state.VenueSessionState;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify live scenario HTTP control-plane routes.
 *
 * <p>Role in system: covers P5-TC-007 API integration for reset/run/summary
 * endpoints without launching Jupyter.</p>
 *
 * <p>Relationships: starts {@link NotebookScenarioHttpServer} with in-memory queue,
 * policy publisher, lifecycle store, and scenario config.</p>
 *
 * <p>Lifecycle: each test owns a local ephemeral HTTP server.</p>
 *
 * <p>Design intent: prove invalid reset requests fail before mutation and
 * successful runs expose visible reset and summary JSON.</p>
 */
final class ScenarioApiIntegrationTest {
    @Test
    void postScenarioResetReturnsVisibleResetSummary() throws Exception {
        final ServerFixture fixture = startServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/reset",
                    "{\"scenarioId\":\"api-reset\",\"seed\":1,\"ticks\":2,\"resetMode\":\"PURGE_AND_REPOPULATE\"}");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"scenarioId\":\"api-reset\""));
            assertTrue(response.body().contains("clearedState"));
            assertTrue(response.body().contains("repopulatedState"));
            assertTrue(fixture.lifecycle.snapshot().stream().anyMatch(event -> event.message.contains("scenario reset api-reset")));
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void postScenarioRunWithPurgeReturnsScenarioSummary() throws Exception {
        final ServerFixture fixture = startServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/run",
                    "{\"scenarioId\":\"api-run\",\"seed\":2,\"ticks\":2,\"resetMode\":\"PURGE_AND_REPOPULATE\","
                            + "\"simulatorGeneratedOrders\":true}");
            final HttpResponse<String> summary = get(fixture, "/scenario/summary");

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"success\":true"));
            assertTrue(response.body().contains("\"summary\""));
            assertTrue(summary.body().contains("\"scenarioId\":\"api-run\""));
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void postScenarioRunWithoutParentOrdersFailsClearlyForLiveMode() throws Exception {
        final ServerFixture fixture = startServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/run",
                    "{\"scenarioId\":\"api-run\",\"seed\":2,\"ticks\":2,\"resetMode\":\"PURGE_AND_REPOPULATE\"}");

            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("parentOrders must not be empty"));
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void postScenarioRunWithParentOrdersReturnsRouteEvidence() throws Exception {
        final ServerFixture fixture = startExecutableServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/run", """
                    {"scenarioId":"api-parent-order","seed":42,"ticks":3,"resetMode":"PURGE_AND_REPOPULATE",
                     "parentOrders":[{"instrumentId":0,"side":1,"quantity":1500,"urgencyId":0,
                       "atTick":1,"submitMode":"SIMULATED","clientOrderRef":"tc-008-buy"}],
                     "maxRouteAttempts":8,"routeTimeoutMillis":1000}
                    """);

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"parentOrderResults\""));
            assertTrue(response.body().contains("\"scenarioId\":\"api-parent-order\""));
            assertTrue(response.body().contains("\"atTick\":1"));
            assertTrue(response.body().contains("\"submitMode\":\"SIMULATED\""));
            assertTrue(response.body().contains("\"clientOrderRef\":\"tc-008-buy\""));
            assertTrue(response.body().contains("\"venueName\":\"VENUE"));
            assertTrue(response.body().contains("\"instrumentSymbol\":\"INST0\""));
            assertTrue(fixture.lifecycle.snapshot().stream().anyMatch(event -> event.message.contains("tc-008-buy")));
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void postScenarioRunAcceptsStringSideValues() throws Exception {
        final ServerFixture fixture = startExecutableServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/run", """
                    {"scenarioId":"api-parent-order-string-side","seed":42,"ticks":3,"resetMode":"PURGE_AND_REPOPULATE",
                     "parentOrders":[{"instrumentId":0,"side":"BUY","quantity":500,"urgencyId":0,
                       "atTick":1,"submitMode":"SIMULATED","clientOrderRef":"string-side-buy"}],
                     "maxRouteAttempts":4,"routeTimeoutMillis":1000}
                    """);

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"sideName\":\"BUY\""));
            assertTrue(response.body().contains("\"clientOrderRef\":\"string-side-buy\""));
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void invalidResetModeFailsBeforeStateMutation() throws Exception {
        final ServerFixture fixture = startServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/reset",
                    "{\"scenarioId\":\"api-bad\",\"seed\":2,\"ticks\":2,\"resetMode\":\"BAD\"}");

            assertEquals(400, response.statusCode());
            assertTrue(response.body().contains("resetMode must be"));
            assertTrue(fixture.lifecycle.snapshot().isEmpty());
        } finally {
            fixture.server.stop();
        }
    }

    @Test
    void failedScenarioRunKeepsActivePolicySafe() throws Exception {
        final ServerFixture fixture = startServer();
        try {
            final HttpResponse<String> response = post(fixture, "/scenario/run",
                    "{\"scenarioId\":\"api-fail\",\"seed\":2,\"ticks\":0,\"resetMode\":\"PURGE_AND_REPOPULATE\"}");

            assertEquals(400, response.statusCode());
            assertNull(fixture.publisher.activePolicy());
        } finally {
            fixture.server.stop();
        }
    }

    private static ServerFixture startServer() throws Exception {
        final InMemoryLifecycleEventStore lifecycle = new InMemoryLifecycleEventStore(32, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final NotebookScenarioHttpServer server = new NotebookScenarioHttpServer(0, new ParentOrderIntentQueue(16), publisher, lifecycle,
                new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true));
        server.start();
        return new ServerFixture(server, publisher, lifecycle);
    }

    private static ServerFixture startExecutableServer() throws Exception {
        final SorConfig config = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(1024);
        final InMemoryLifecycleEventStore lifecycle = new InMemoryLifecycleEventStore(256, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final MarketBookState market = baselineMarket(config);
        final VenueSessionState sessions = baselineSessions(config);
        final RiskLimitSnapshot risk = baselineRisk(config);
        publisher.publish(
                new InitialPolicyBootstrap().bootstrap(config),
                PolicyValidationReport.validReport(),
                new PolicyLintReport(java.util.List.of()),
                new PolicyDiff(),
                1,
                1L
        );
        lifecycle.append(new LifecycleEvent(1L, 1L, 1, LifecycleEventType.METRICS_SUMMARY, 1L,
                "scenario api test runtime ready"));
        final NotebookScenarioHttpServer server = new NotebookScenarioHttpServer(0, queue, publisher, lifecycle, config,
                new PolicyDrivenSorExecutioner(publisher, market, sessions, risk),
                new ChildOrderBuffer(Math.max(1, Math.min(16, config.venueCount()))),
                market,
                InstrumentMetadata.simulated(config.instrumentCount()),
                VenueMetadata.simulated(config.instrumentCount(), config.venueCount()));
        server.start();
        return new ServerFixture(server, publisher, lifecycle);
    }

    private static MarketBookState baselineMarket(final SorConfig config) {
        final MarketBookState market = new MarketBookState(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final long mid = 10_000L + instrumentId * 10L + venueId;
                final long displayedQty = 5_000L + venueId * 100L;
                market.updateTopOfBook(instrumentId, venueId, mid - 1L, mid + 1L, displayedQty, displayedQty);
            }
        }
        return market;
    }

    private static VenueSessionState baselineSessions(final SorConfig config) {
        final VenueSessionState sessions = new VenueSessionState(config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            sessions.setStatus(venueId, VenueStatus.OPEN);
        }
        return sessions;
    }

    private static RiskLimitSnapshot baselineRisk(final SorConfig config) {
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            risk.setMaxChildQty(instrumentId, 1_000L);
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                risk.setVenueLimits(instrumentId, venueId, 10_000_000L, 2_500);
            }
        }
        return risk;
    }

    private static HttpResponse<String> post(final ServerFixture fixture, final String path, final String body) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(fixture.baseUrl() + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(final ServerFixture fixture, final String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(fixture.baseUrl() + path))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private record ServerFixture(
            NotebookScenarioHttpServer server,
            PolicyPublisher publisher,
            InMemoryLifecycleEventStore lifecycle
    ) {
        String baseUrl() {
            return "http://127.0.0.1:" + server.port();
        }
    }
}
