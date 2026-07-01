package com.nitroj.sor.testserver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies HelmChartLint behavior for the runnable SOR test server.
 *
 * <p>Run with :sor-test-server:test to protect notebook, container, and integration-server workflows.</p>
 */
class HelmChartLintTest {
    @Test
    void chartContainsDeploymentServiceProbesAndNetworkPolicy() throws Exception {
        final Path chart = Path.of("sor-test-server/helm/adaptive-quantum-sor");
        assertTrue(Files.isRegularFile(chart.resolve("Chart.yaml")));
        final String deployment = Files.readString(chart.resolve("templates/deployment.yaml"));
        final String service = Files.readString(chart.resolve("templates/service.yaml"));
        final String networkPolicy = Files.readString(chart.resolve("templates/networkpolicy.yaml"));
        assertTrue(deployment.contains("livenessProbe"));
        assertTrue(deployment.contains("/ready"));
        assertTrue(service.contains("http-control"));
        assertTrue(networkPolicy.contains("NetworkPolicy"));
    }
}
