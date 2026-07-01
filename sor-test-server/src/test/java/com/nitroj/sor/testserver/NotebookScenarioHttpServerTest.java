package com.nitroj.sor.testserver;

import com.nitroj.sor.testkit.TestPolicyFixtures;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;
import com.nitroj.sor.core.model.ParentOrderIntentQueue;
import com.nitroj.sor.core.model.Side;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.publication.PublicationGate;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify the Adaptive Quantum SOR HTTP API server.
 *
 * <p>Role in system: covers order submission/validation, status lookup, stats,
 * policy identity, lifecycle stream, and API failure isolation.</p>
 *
 * <p>Relationships: starts {@link NotebookScenarioHttpServer} with in-memory queue,
 * publisher, and lifecycle store.</p>
 *
 * <p>Lifecycle: starts a local ephemeral HTTP server per integration test.</p>
 *
 * <p>Design intent: use the JDK HTTP client/server stack to keep the Adaptive Quantum SOR
 * dependency-free.</p>
 */
final class NotebookScenarioHttpServerTest {
    @Test
    void httpEndpointsSupportNotebookWorkflow() throws Exception {
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(4);
        final InMemoryLifecycleEventStore store = new InMemoryLifecycleEventStore(8, true);
        final PolicyPublisher publisher = publisher();
        final NotebookScenarioHttpServer server = new NotebookScenarioHttpServer(0, queue, publisher, store);
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final String base = "http://127.0.0.1:" + server.port();
            final HttpResponse<String> order = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"instrumentId\":0,\"side\":1,\"quantity\":100,\"urgencyId\":0}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> status = client.send(HttpRequest.newBuilder(URI.create(base + "/orders/1")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> stats = client.send(HttpRequest.newBuilder(URI.create(base + "/stats/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> policy = client.send(HttpRequest.newBuilder(URI.create(base + "/policy/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> events = client.send(HttpRequest.newBuilder(URI.create(base + "/events/stream")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, order.statusCode());
            assertTrue(order.body().contains("\"parentOrderId\":1"));
            assertEquals(1, queue.size());
            assertEquals(200, status.statusCode());
            assertTrue(status.body().contains("\"remainingQty\":100"));
            assertTrue(stats.body().contains("venueCount"));
            assertTrue(policy.body().contains("policyVersion"));
            assertTrue(events.body().contains("order accepted"));
        } finally {
            server.stop();
        }
    }

    @Test
    void invalidOrderRejectedAndApiFailureDoesNotStopEngineState() throws Exception {
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(2);
        final NotebookScenarioHttpServer server = new NotebookScenarioHttpServer(0, queue, publisher(), new InMemoryLifecycleEventStore(2, true));
        server.start();
        try {
            final HttpResponse<String> invalid = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/orders"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"instrumentId\":0,\"side\":99,\"quantity\":100,\"urgencyId\":0}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(400, invalid.statusCode());
            assertEquals(0, queue.size());
        } finally {
            server.stop();
        }
    }

    @Test
    void requestViewsAndEventStreamContractsAreCovered() {
        final OrderRequest request = new OrderRequest(0, Side.BUY, 10, 0);
        final OrderRequest stringSideRequest = OrderRequest.parse(
                "{\"instrumentId\":0,\"side\":\"BUY\",\"quantity\":10,\"urgencyId\":0}");
        final OrderStatusView status = new OrderStatusView();
        status.parentOrderId = 1L;
        status.remainingQty = 10L;
        final StatsSnapshotView stats = new StatsSnapshotView();
        stats.policyVersion = 2L;
        stats.venueCount = 3;
        final InMemoryLifecycleEventStore store = new InMemoryLifecycleEventStore(2, true);
        store.append(new LifecycleEvent(1, 1, 1, LifecycleEventType.SOR_DECISION, 1, "hello"));

        assertEquals(10L, request.toIntent(1, 1).quantity);
        assertEquals(Side.BUY, stringSideRequest.side());
        assertTrue(status.toJson().contains("parentOrderId"));
        assertTrue(stats.toJson().contains("venueCount"));
        assertTrue(new EventStreamHandler(store).render().contains("hello"));
        assertThrows(IllegalArgumentException.class, () -> OrderRequest.parse("{}"));
    }

    private static PolicyPublisher publisher() {
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(TestPolicyFixtures.policy(), PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.sor.core.governance.PolicyDiff(), 10, 1L);
        return publisher;
    }
}
