package com.nitroj.sor.testserver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies NetworkPolicyEnforcement behavior for the runnable SOR test server.
 *
 * <p>Run with :sor-test-server:test to protect notebook, container, and integration-server workflows.</p>
 */
class NetworkPolicyEnforcementTest {
    @Test
    void networkPolicyRestrictsIngressToOmsNamespace() throws Exception {
        final String policy = Files.readString(Path.of(
                "sor-test-server/helm/adaptive-quantum-sor/templates/networkpolicy.yaml"));
        assertTrue(policy.contains("policyTypes"));
        assertTrue(policy.contains("namespaceSelector"));
        assertTrue(policy.contains("omsNamespace"));
    }
}
