package com.nitroj.sor.obs;

import com.nitroj.sor.api.Observability;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Default Phase 8 observability backend with primitive counters and histograms. */
public final class SorObservability implements Observability {
    private static final String[] METER_NAMES = {
            "sor.route.decision.latency",
            "sor.parent.order.ring.depth",
            "sor.child.order.ring.depth",
            "sor.backpressure.rejections",
            "sor.policy.publications",
            "sor.policy.publication.duration",
            "sor.optimizer.cycle.duration"
    };

    private final RouteDecisionLatencyRecorder routeLatency = new RouteDecisionLatencyRecorder();
    private final AtomicLong ringDepth = new AtomicLong();
    private final AtomicLong backpressure = new AtomicLong();
    private final AtomicLong policyPublications = new AtomicLong();
    private final AtomicLong lastPolicyVersion = new AtomicLong();
    private final AtomicLong lastPolicyHash64 = new AtomicLong();
    private final AtomicLong policyPublicationDuration = new AtomicLong();
    private final PolicyOptimizationSpans spans = new PolicyOptimizationSpans();
    private final Set<String> meterNames = new LinkedHashSet<>(Arrays.asList(METER_NAMES));

    public static SorObservability create() {
        return new SorObservability();
    }

    @Override
    public void recordRouteDecisionLatency(final long nanos) {
        routeLatency.record(nanos);
    }

    @Override
    public void recordParentOrderRingDepth(final int depth) {
        ringDepth.set(depth);
    }

    @Override
    public void recordBackpressureRejected(final int reasonCode) {
        backpressure.incrementAndGet();
    }

    @Override
    public void recordPolicyPublished(final long policyVersion, final long policyHash64, final long durationNanos) {
        policyPublications.incrementAndGet();
        lastPolicyVersion.set(policyVersion);
        lastPolicyHash64.set(policyHash64);
        policyPublicationDuration.set(durationNanos);
        spans.publish(policyVersion, policyHash64);
    }

    public RouteDecisionLatencyRecorder routeLatency() {
        return routeLatency;
    }

    public PolicyOptimizationSpans spans() {
        return spans;
    }

    public Set<String> meterNames() {
        return Set.copyOf(meterNames);
    }

    public long backpressureRejections() {
        return backpressure.get();
    }

    public long policyPublications() {
        return policyPublications.get();
    }

    public long parentOrderRingDepth() {
        return ringDepth.get();
    }

    @Override
    public String prometheusText() {
        return """
                # TYPE sor_route_decision_latency summary
                sor_route_decision_latency_count %d
                sor_route_decision_latency_p50 %d
                sor_route_decision_latency_p99 %d
                sor_route_decision_latency_p999 %d
                # TYPE sor_parent_order_ring_depth gauge
                sor_parent_order_ring_depth %d
                # TYPE sor_child_order_ring_depth gauge
                sor_child_order_ring_depth{venue="0"} 0
                # TYPE sor_backpressure_rejections counter
                sor_backpressure_rejections_total %d
                # TYPE sor_policy_publications counter
                sor_policy_publications_total{policy_version="%d",policy_hash64="%d"} %d
                # TYPE sor_policy_publication_duration gauge
                sor_policy_publication_duration %d
                # TYPE sor_optimizer_cycle_duration summary
                sor_optimizer_cycle_duration_count 0
                """.formatted(
                routeLatency.count(),
                routeLatency.percentile(0.50),
                routeLatency.percentile(0.99),
                routeLatency.percentile(0.999),
                ringDepth.get(),
                backpressure.get(),
                lastPolicyVersion.get(),
                lastPolicyHash64.get(),
                policyPublications.get(),
                policyPublicationDuration.get());
    }
}
