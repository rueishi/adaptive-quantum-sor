package com.nitroj.sor.http;

import com.nitroj.sor.obs.SorObservability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsExpositionTest {
    @Test
    void metricsEndpointReturnsPrometheusText() throws Exception {
        final SorObservability observability = SorObservability.create();
        observability.recordPolicyPublished(1, 2, 3);
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, new FakeEngine(), observability);
        server.start();
        try {
            final var response = HttpTestClient.get("http://127.0.0.1:" + server.port() + "/metrics");
            assertEquals(200, response.status());
            assertTrue(response.contentType().startsWith("text/plain; version=0.0.4"));
            assertTrue(response.body().contains("sor_policy_publications_total"));
        } finally {
            server.stop();
        }
    }
}
