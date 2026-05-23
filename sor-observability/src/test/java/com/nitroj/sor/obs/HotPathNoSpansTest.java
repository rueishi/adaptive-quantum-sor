package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
