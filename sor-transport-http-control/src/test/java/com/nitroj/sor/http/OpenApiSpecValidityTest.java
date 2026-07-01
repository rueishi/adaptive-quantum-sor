package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the embedded OpenAPI document for HTTP control endpoints.
 *
 * <p>Run with HTTP transport tests before adding, renaming, or removing endpoints.</p>
 */
class OpenApiSpecValidityTest {
    @Test
    void openApiEndpointReturnsVersion31Document() throws Exception {
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, new FakeEngine(), Observability.noop());
        server.start();
        try {
            final var response = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/openapi.json");
            assertEquals(200, response.status());
            assertTrue(response.body().contains("\"openapi\":\"3.1.0\""));
            assertTrue(response.body().contains("\"/orders\""));
            assertTrue(response.body().contains("\"/orders/{id}\""));
            assertTrue(response.body().contains("\"/metrics\""));
            assertTrue(response.body().contains("\"/policy/current\""));
            assertTrue(response.body().contains("\"/control/state\""));
            assertTrue(response.body().contains("\"/control/market-data\""));
            assertTrue(response.body().contains("\"/control/reset\""));
            assertTrue(!response.body().contains("/scenario/"));
        } finally {
            server.stop();
        }
    }
}
