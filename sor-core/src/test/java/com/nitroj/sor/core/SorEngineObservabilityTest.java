package com.nitroj.sor.core;

import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies sor engine observability behavior for embedded SOR engine implementation and public API wiring.
 *
 * <p>Run with :sor-core:test to protect engine bootstrap, order submission, reset, diagnostics, and tests.</p>
 */
class SorEngineObservabilityTest {
    @Test
    void engineReportsPolicyRouteDepthAndBackpressureSignals() {
        final CapturingObservability observability = new CapturingObservability();
        final SorEngineImpl engine = (SorEngineImpl) EngineTestSupport.builder()
                .config(new com.nitroj.sor.api.SorConfig(1, 100))
                .observability(observability)
                .build();
        engine.warmup(0);
        engine.submitParentOrder(ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(1).urgency(0).build());
        try {
            engine.submitParentOrder(ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(1).urgency(0).build());
        } catch (com.nitroj.sor.api.BackpressureException expected) {
            // expected
        }

        assertEquals(1, observability.policyPublications.get());
        assertEquals(1, observability.depth.get());
        assertEquals(1, observability.backpressure.get());
        assertTrue(observability.routeLatency.get() >= 0);
        engine.close();
    }

    private static final class CapturingObservability implements Observability {
        final AtomicLong routeLatency = new AtomicLong(-1);
        final AtomicLong depth = new AtomicLong();
        final AtomicLong backpressure = new AtomicLong();
        final AtomicLong policyPublications = new AtomicLong();
        @Override public void recordRouteDecisionLatency(final long nanos) { routeLatency.set(nanos); }
        @Override public void recordParentOrderRingDepth(final int depth) { this.depth.set(depth); }
        @Override public void recordBackpressureRejected(final int reasonCode) { backpressure.incrementAndGet(); }
        @Override public void recordPolicyPublished(final long policyVersion, final long policyHash64, final long durationNanos) { policyPublications.incrementAndGet(); }
    }
}
