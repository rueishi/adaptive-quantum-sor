package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies hot-path routing telemetry does not create tracing spans.
 *
 * <p>Run with observability tests to keep latency-sensitive paths free of control-plane span overhead.</p>
 */
class HotPathNoSpansTest {
    @Test
    void routeObservationsDoNotCreateSpans() {
        final SorObservability observability = SorObservability.create();

        for (int i = 0; i < 10_000; i++) {
            observability.recordRouteDecisionLatency(i);
        }

        assertTrue(observability.spans().spans().isEmpty());
    }
}
