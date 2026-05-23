package com.nitroj.sor.api;

enum NoopObservability implements Observability {
    INSTANCE;

    @Override public void recordRouteDecisionLatency(final long nanos) {}
    @Override public void recordParentOrderRingDepth(final int depth) {}
    @Override public void recordBackpressureRejected(final int reasonCode) {}
    @Override public void recordPolicyPublished(final long policyVersion, final long policyHash64, final long durationNanos) {}
}
