package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;

/**
 * Responsibility: perform deterministic timer-driven parent-order reslices.
 *
 * <p>Role in system: if a parent has residual quantity, the scheduler creates a
 * new route decision using the current active policy captured by the executioner.</p>
 *
 * <p>Relationships: delegates routing to {@link PolicyDrivenSorExecutioner} and
 * updates {@link ParentOrderState}.</p>
 *
 * <p>Lifecycle: called on a reslice timer while a parent order remains open.</p>
 *
 * <p>Design intent: each reslice is an independent route decision, so policy
 * versions may differ across a parent lifecycle while old child orders remain
 * valid.</p>
 */
public final class ResliceScheduler {
    private final PolicyDrivenSorExecutioner executioner;

    public ResliceScheduler(final PolicyDrivenSorExecutioner executioner) {
        if (executioner == null) {
            throw new IllegalArgumentException("executioner must not be null");
        }
        this.executioner = executioner;
    }

    /** Performs a reslice only when residual quantity is positive. */
    public ResliceDecision onTimer(final ParentOrderState parentState, final OrderIntent original, final ChildOrderBuffer output) {
        if (parentState == null || original == null || output == null) {
            throw new IllegalArgumentException("reslice inputs must not be null");
        }
        if (parentState.residualQty() == 0) {
            return new ResliceDecision(false, null);
        }
        final OrderIntent residualIntent = new OrderIntent(original.parentOrderId, original.instrumentId, original.side,
                parentState.residualQty(), original.urgencyId, original.createdAtNanos);
        final RouteDecisionResult result = executioner.route(residualIntent, output);
        parentState.apply(result);
        return new ResliceDecision(true, result);
    }
}
