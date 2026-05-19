package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.SorEngineRuntime;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the one-command notebook launcher assets.
 *
 * <p>Role in system: proves the startup script has a real long-running API
 * target and references the Jupyter notebooks expected by users.</p>
 *
 * <p>Relationships: starts {@link SorEngineRuntime} and inspects
 * {@code scripts/start-jupyter-lab.sh}.</p>
 *
 * <p>Lifecycle: executed by Gradle without launching an actual Jupyter kernel
 * or browser.</p>
 *
 * <p>Design intent: keep the user-facing launcher testable while avoiding GUI
 * or notebook runtime dependencies in CI.</p>
 */
final class NotebookDemoApiLauncherTest {
    @Test
    void demoApiServerSupportsNotebookEndpoints() throws Exception {
        final SorEngineRuntime runtime = SorEngineRuntime.create(new SorConfig(3, 5, 4, 2, SorConfig.RuntimeMode.DEMO, true), 0);
        runtime.start();
        try {
            final String base = "http://127.0.0.1:" + runtime.apiPort();
            final HttpClient client = HttpClient.newHttpClient();
            final HttpResponse<String> stats = client.send(HttpRequest.newBuilder(URI.create(base + "/stats/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> policy = client.send(HttpRequest.newBuilder(URI.create(base + "/policy/current")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> order = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"instrumentId\":0,\"side\":1,\"quantity\":100,\"urgencyId\":0}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            final HttpResponse<String> orderSummary = client.send(HttpRequest.newBuilder(URI.create(base + "/orders/summary")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, stats.statusCode());
            assertTrue(stats.body().contains("\"venueCount\":5"));
            assertEquals(200, policy.statusCode());
            assertTrue(policy.body().contains("policyVersion"));
            assertEquals(200, order.statusCode());
            assertTrue(order.body().contains("parentOrderId"));
            assertTrue(order.body().contains("\"remainingQty\":0"));
            assertTrue(order.body().contains("\"childOrderCount\":1"));
            assertEquals(200, orderSummary.statusCode());
            assertTrue(orderSummary.body().contains("\"parentOrderCount\":1"));
            assertTrue(orderSummary.body().contains("\"byVenue\""));
            assertTrue(orderSummary.body().contains("\"byInstrumentVenue\""));
            assertTrue(orderSummary.body().contains("\"filledQty\":100"));
        } finally {
            runtime.close();
        }
    }

    @Test
    void engineScenarioRunRoutesSuppliedParentOrders() throws Exception {
        final SorEngineRuntime runtime = SorEngineRuntime.create(new SorConfig(3, 5, 4, 2, SorConfig.RuntimeMode.DEMO, true), 0);
        runtime.start();
        try {
            final String base = "http://127.0.0.1:" + runtime.apiPort();
            final HttpResponse<String> run = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/scenario/run"))
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {"scenarioId":"api-parent-order","seed":42,"ticks":3,"resetMode":"PURGE_AND_REPOPULATE",
                             "parentOrders":[{"instrumentId":0,"side":1,"quantity":4500,"urgencyId":0}],
                             "maxRouteAttempts":8,"routeTimeoutMillis":1000}
                            """))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertEquals(200, run.statusCode());
            assertTrue(run.body().contains("\"parentOrderResults\""));
            assertTrue(run.body().contains("\"remainingQty\":0"));
            assertTrue(run.body().contains("\"childOrderCount\":5"));
            assertTrue(run.body().contains("\"routeAttempts\":2"));
            assertTrue(run.body().contains("\"venueId\":"));
            assertTrue(run.body().contains("\"venueName\":\"VENUE"));
            assertTrue(run.body().contains("\"instrumentSymbol\":\"INST0\""));
            assertTrue(run.body().contains("\"sideName\":\"BUY\""));
            assertTrue(run.body().contains("\"priceTicks\":"));
            assertTrue(run.body().contains("\"notionalTicks\":"));

            final HttpResponse<String> orderSummary = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/orders/summary"))
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, orderSummary.statusCode());
            assertTrue(orderSummary.body().contains("\"parentOrderCount\":1"));
            assertTrue(orderSummary.body().contains("\"childOrderCount\":5"));
            assertTrue(orderSummary.body().contains("\"instrumentSymbol\":\"INST0\""));
            assertTrue(orderSummary.body().contains("\"venueName\":\"VENUE"));
        } finally {
            runtime.close();
        }
    }

    @Test
    void startupScriptOpensAllNotebookPanels() throws Exception {
        final String script = Files.readString(Path.of("scripts/start-jupyter-lab.sh"));

        assertTrue(script.contains("AdaptiveQuantumSorApplication --api-port"));
        assertTrue(script.contains("notebooks/submit_parent_order.ipynb"));
        assertTrue(script.contains("notebooks/live_stats_monitor.ipynb"));
        assertTrue(script.contains("notebooks/scenario_runner.ipynb"));
        assertTrue(script.contains("three notebook panels"));
        assertTrue(script.contains("PYTHONPATH"));
        assertTrue(script.contains("python/examples/sor_notebook_features_large.csv"));
    }
}
