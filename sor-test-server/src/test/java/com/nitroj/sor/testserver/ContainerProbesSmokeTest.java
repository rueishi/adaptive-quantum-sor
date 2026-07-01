package com.nitroj.sor.testserver;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies ContainerProbesSmoke behavior for the runnable SOR test server.
 *
 * <p>Run with :sor-test-server:test to protect notebook, container, and integration-server workflows.</p>
 */
class ContainerProbesSmokeTest {
    @Test
    void serverStartsAndPassesProbesAndOrderPath() throws Exception {
        final var options = SimulatorServerApplication.Options.parse(new String[]{
                "--transport=embedded-only",
                "--http-control-port=0",
                "--warmup-orders=1"
        });
        try (var runtime = SimulatorServerApplication.start(options)) {
            final HttpClient client = HttpClient.newHttpClient();
            final String base = "http://127.0.0.1:" + runtime.httpPort();
            assertEquals(200, get(client, base + "/healthz").statusCode());
            assertEquals(200, get(client, base + "/ready").statusCode());
            final HttpResponse<String> order = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"instrumentId\":7,\"side\":1,\"quantity\":1000,\"urgencyId\":2}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, order.statusCode());
            assertTrue(order.body().contains("\"parentOrderId\""));
        }
    }

    private static HttpResponse<String> get(final HttpClient client, final String uri) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(uri)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
