package com.nitroj.sor.api.spi;

/**
 * Responsibility: synchronous pre-trade risk provider.
 *
 * <p>Role in system: the engine calls this on the routing path with
 * pre-computed risk state owned by the integrator.</p>
 *
 * <p>Relationships: reads {@link RiskCheckRequest} and populates
 * {@link RiskDecision}.</p>
 *
 * <p>Lifecycle: configured before engine build and reused after warmup.</p>
 *
 * <p>Design intent: forbid RPC-style risk checks from the hot path.</p>
 */
@FunctionalInterface
public interface RiskProvider {
    /**
     * Performs a synchronous risk check.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Must return within
     * the documented nanosecond risk budget.</p>
     *
     * @param request reusable request populated by the engine
     * @param decision reusable decision populated by the provider
     */
    void check(RiskCheckRequest request, RiskDecision decision);
}
