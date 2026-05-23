package com.nitroj.sor.api;

/**
 * Responsibility: lightweight observability seam used by engine hot paths.
 *
 * <p>Role in system: lets integrators wire metrics and warm-path spans without
 * making core depend on Micrometer, OpenTelemetry, or any concrete backend.</p>
 *
 * <p>Relationships: supplied through {@link SorEngineBuilder#observability} and
 * implemented by `sor-observability` or by integrator adapters.</p>
 *
 * <p>Lifecycle: configured before engine build and invoked for route, policy,
 * ring-depth, and backpressure signals.</p>
 *
 * <p>Design intent: preserve hot-path zero-allocation by passing primitive
 * observations into an already-constructed backend.</p>
 */
public interface Observability {
    /**
     * Records route-decision latency in nanoseconds.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    void recordRouteDecisionLatency(long nanos);

    /**
     * Records current parent-order ring depth.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    void recordParentOrderRingDepth(int depth);

    /**
     * Records a parent-order backpressure rejection.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    void recordBackpressureRejected(int reasonCode);

    /**
     * Records a policy publication event.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    void recordPolicyPublished(long policyVersion, long policyHash64, long durationNanos);

    /**
     * Renders metrics in Prometheus text exposition format when supported.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    default String prometheusText() {
        return "";
    }

    /**
     * Returns an allocation-free no-op implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    static Observability noop() {
        return NoopObservability.INSTANCE;
    }
}
