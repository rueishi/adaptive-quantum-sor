package com.nitroj.sor.sim.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PrometheusScrapeIntegrationTest {
    @Test
    void serviceMonitorScrapesMetricsEndpoint() throws Exception {
        final String monitor = Files.readString(Path.of(
                "sor-test-server/helm/adaptive-quantum-sor/templates/servicemonitor.yaml"));
        assertTrue(monitor.contains("ServiceMonitor"));
        assertTrue(monitor.contains("/metrics"));
    }
}
