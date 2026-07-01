package com.nitroj.sor.core.execution;

/**
 * Responsibility: summarize a reslice attempt.
 *
 * <p>Role in system: reslice scheduling needs to expose whether a timer event
 * actually routed residual quantity and which policy version was captured.</p>
 *
 * <p>Relationships: produced by {@link ResliceScheduler} and wraps
 * {@link RouteDecisionResult} when routing occurs.</p>
 *
 * <p>Lifecycle: immutable after each reslice timer event.</p>
 *
 * <p>Design intent: keep no-op reslices explicit for tests and audit.</p>
 */
public final class ResliceDecision {
    public final boolean resliced;
    public final RouteDecisionResult routeDecisionResult;

    public ResliceDecision(final boolean resliced, final RouteDecisionResult routeDecisionResult) {
        this.resliced = resliced;
        this.routeDecisionResult = routeDecisionResult;
    }
}
