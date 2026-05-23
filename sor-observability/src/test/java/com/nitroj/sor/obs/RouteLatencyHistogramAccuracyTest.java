package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RouteLatencyHistogramAccuracyTest {
    @Test
    void percentilesMatchDeterministicReference() {
        final RouteDecisionLatencyRecorder recorder = new RouteDecisionLatencyRecorder();
        for (int i = 1; i <= 10_000; i++) {
            recorder.record(i);
        }

        assertEquals(5_000, recorder.percentile(0.50));
        assertEquals(9_900, recorder.percentile(0.99));
        assertEquals(9_990, recorder.percentile(0.999));
    }
}
