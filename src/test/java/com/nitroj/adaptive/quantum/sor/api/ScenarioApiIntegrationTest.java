package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
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
 * <p>Relationships: starts {@link SorHttpApiServer} with in-memory queue,
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
                    "{\"scenarioId\":\"api-run\",\"seed\":2,\"ticks\":2,\"resetMode\":\"PURGE_AND_REPOPULATE\"}");
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
        final SorHttpApiServer server = new SorHttpApiServer(0, new ParentOrderIntentQueue(16), publisher, lifecycle,
                new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true));
        server.start();
        return new ServerFixture(server, publisher, lifecycle);
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
            SorHttpApiServer server,
            PolicyPublisher publisher,
            InMemoryLifecycleEventStore lifecycle
    ) {
        String baseUrl() {
            return "http://127.0.0.1:" + server.port();
        }
    }
}
