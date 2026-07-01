package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: executable example for embedding the built-in HTTP control
 * server.
 *
 * <p>Role in system: shows users can start the HTTP module from a plain
 * `SorEngine` without depending on `sor-test-server`.</p>
 *
 * <p>Relationships: composes a tiny fake engine, {@link Observability}, and
 * {@link HttpControlPlaneServer}.</p>
 *
 * <p>Lifecycle: runs as a regular unit test and doubles as bootstrap
 * documentation.</p>
 *
 * <p>Design intent: keep user HTTP startup separate from simulator/demo
 * infrastructure.</p>
 */
class HttpControlPlaneServerBootstrapExampleTest {
    @Test
    void userCanStartBuiltInHttpControlServerAroundEngine() throws Exception {
        final FakeEngine engine = new FakeEngine();
        engine.ready = true;
        final Observability observability = Observability.noop();
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, engine, observability);

        server.start();
        try {
            final var health = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/healthz");
            final var ready = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/ready");

            assertEquals(200, health.status());
            assertEquals(200, ready.status());
        } finally {
            server.stop();
        }
    }
}
