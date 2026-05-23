package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterRegistryShapeTest {
    @Test
    void exposesDocumentedMeterSet() {
        final Set<String> meters = new SorMeterRegistry(SorObservability.create()).getMeters();

        assertTrue(meters.contains("sor.route.decision.latency"));
        assertTrue(meters.contains("sor.parent.order.ring.depth"));
        assertTrue(meters.contains("sor.child.order.ring.depth"));
        assertTrue(meters.contains("sor.backpressure.rejections"));
        assertTrue(meters.contains("sor.policy.publications"));
        assertTrue(meters.contains("sor.policy.publication.duration"));
        assertTrue(meters.contains("sor.optimizer.cycle.duration"));
    }
}
