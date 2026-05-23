package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiSpecValidityTest {
    @Test
    void openApiEndpointReturnsVersion31Document() throws Exception {
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, new FakeEngine(), Observability.noop());
        server.start();
        try {
            final var response = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/openapi.json");
            assertEquals(200, response.status());
            assertTrue(response.body().contains("\"openapi\":\"3.1.0\""));
            assertTrue(response.body().contains("\"/metrics\""));
        } finally {
            server.stop();
        }
    }
}
