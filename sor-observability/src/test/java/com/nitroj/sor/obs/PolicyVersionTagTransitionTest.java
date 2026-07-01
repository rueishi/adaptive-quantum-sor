package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies policy-version tags transition correctly across publications.
 *
 * <p>Run with observability tests to keep policy lineage metrics accurate.</p>
 */
class PolicyVersionTagTransitionTest {
    @Test
    void metricsRemainValidAcrossPolicyVersionBumps() {
        final SorObservability observability = SorObservability.create();

        observability.recordPolicyPublished(1, 11, 5);
        final String first = observability.prometheusText();
        observability.recordPolicyPublished(2, 22, 6);
        final String second = observability.prometheusText();

        assertTrue(first.contains("policy_version=\"1\""));
        assertTrue(second.contains("policy_version=\"2\""));
        assertTrue(second.contains("sor_policy_publications_total"));
    }
}
