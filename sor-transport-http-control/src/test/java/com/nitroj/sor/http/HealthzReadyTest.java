package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies health and readiness endpoints exposed by the HTTP control plane.
 *
 * <p>Run with HTTP transport tests before changing liveness or readiness behavior.</p>
 */
class HealthzReadyTest {
    @Test
    void healthzIsAliveAndReadyMirrorsEngine() throws Exception {
        final FakeEngine engine = new FakeEngine();
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, engine, Observability.noop());
        server.start();
        try {
            assertEquals(200, HttpTestClient.get("http://127.0.0.1:" + server.port() + "/healthz").status());
            assertEquals(503, HttpTestClient.get("http://127.0.0.1:" + server.port() + "/ready").status());
            engine.ready = true;
            final var ready = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/ready");
            assertEquals(200, ready.status());
            assertTrue(ready.body().contains("true"));
        } finally {
            server.stop();
        }
    }
}
