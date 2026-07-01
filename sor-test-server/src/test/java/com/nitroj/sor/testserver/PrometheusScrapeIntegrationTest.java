package com.nitroj.sor.testserver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies PrometheusScrapeIntegration behavior for the runnable SOR test server.
 *
 * <p>Run with :sor-test-server:test to protect notebook, container, and integration-server workflows.</p>
 */
class PrometheusScrapeIntegrationTest {
    @Test
    void serviceMonitorScrapesMetricsEndpoint() throws Exception {
        final String monitor = Files.readString(Path.of(
                "sor-test-server/helm/adaptive-quantum-sor/templates/servicemonitor.yaml"));
        assertTrue(monitor.contains("ServiceMonitor"));
        assertTrue(monitor.contains("/metrics"));
    }
}
