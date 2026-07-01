package com.nitroj.sor.api;

/**
 * Responsibility: control-plane operations for an engine instance.
 *
 * <p>Role in system: separates reset and recovery-style lifecycle operations
 * from the hot-path {@link SorEngine} order-submission contract.</p>
 *
 * <p>Relationships: implemented by embedded engines and future remote control
 * transports; consumes {@link SorResetRequest} and returns
 * {@link SorResetSummary} evidence.</p>
 *
 * <p>Lifecycle: resolved after engine construction and used by operators,
 * scenario runners, or recovery code, never by per-order routing.</p>
 *
 * <p>Design intent: make destructive state changes explicit, typed, audited,
 * and safety-gated.</p>
 */
public interface SorControlPlane {
    /**
     * Applies a typed reset request.
     *
     * <p>Control-plane method, not hot-path. Implementations may allocate and
     * block while auditing or clearing state.</p>
     *
     * @param request reset request
     * @return reset evidence summary
     */
    SorResetSummary reset(SorResetRequest request);

    /**
     * Reads a compact immutable summary of engine-owned runtime state.
     *
     * <p>Control-plane method, not hot-path. Implementations must not return
     * mutable engine internals.</p>
     *
     * @return state summary
     */
    SorStateSummary stateSummary();

    /**
     * Reads a compact immutable summary of engine-owned market data state.
     *
     * <p>Control-plane method, not hot-path. Implementations must not return
     * mutable market-book arrays.</p>
     *
     * @return market data snapshot summary
     */
    MarketDataSnapshotSummary marketDataSnapshot();
}
