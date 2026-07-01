package com.nitroj.sor.api;

/**
 * Responsibility: public readiness phase for an engine instance.
 *
 * <p>Role in system: distinguishes JIT warmup, market hydration, order
 * reconciliation, ready, and fail-closed recovery states for control-plane
 * diagnostics and not-ready rejection.</p>
 *
 * <p>Relationships: exposed by {@link SorStateSummary} and used by
 * {@link SorNotReadyException}.</p>
 *
 * <p>Lifecycle: phase values are emitted by the engine as startup, hydration,
 * reset, and recovery lifecycle operations progress.</p>
 *
 * <p>Design intent: replace ambiguous boolean readiness in diagnostics with a
 * typed phase vocabulary.</p>
 */
public enum SorReadinessPhase {
    /** Engine object has been constructed but not warmed or hydrated. */
    CONSTRUCTED,
    /** JIT/risk warmup has completed but startup hydration has not committed. */
    WARMED,
    /** Engine is applying or waiting for market-data startup state. */
    MARKET_HYDRATING,
    /** Engine is reconciling OMS/EMS order-state snapshots. */
    ORDER_RECONCILING,
    /** Engine can accept new parent order intent. */
    READY,
    /** Engine failed startup or hydration and requires operator action. */
    FAILED,
    /** Engine requires recovery before it can accept new parent order intent. */
    RECOVERY_REQUIRED
}
