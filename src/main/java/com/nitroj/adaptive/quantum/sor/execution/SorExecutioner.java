package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;

/**
 * Responsibility: define common SOR executioner behavior.
 *
 * <p>Role in system: adaptive and static SOR implementations share the same
 * parent-order input and child-buffer output shape for comparison.</p>
 *
 * <p>Relationships: implemented by {@link PolicyDrivenSorExecutioner} and
 * {@link StaticSorExecutioner}.</p>
 *
 * <p>Lifecycle: executioners are created during startup/test setup and called
 * per parent order or reslice event.</p>
 *
 * <p>Design intent: common interface keeps metrics and comparison code agnostic
 * to routing strategy.</p>
 */
public interface SorExecutioner {
    /** Routes one parent order into the provided child-order buffer. */
    RouteDecisionResult route(OrderIntent intent, ChildOrderBuffer output);
}
