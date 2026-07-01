package com.nitroj.sor.api;

/**
 * Responsibility: enumerate supported control-plane reset modes.
 *
 * <p>Role in system: callers choose exactly how much runtime state may be
 * cleared, preserved, or appended during scenario and recovery operations.</p>
 *
 * <p>Relationships: used by {@link SorResetRequest} and reported by
 * {@link SorResetSummary}.</p>
 *
 * <p>Lifecycle: stable additive enum for control-plane operations.</p>
 *
 * <p>Design intent: avoid an ambiguous boolean reset flag for destructive
 * trading-engine state changes.</p>
 */
public enum SorResetMode {
    /** Clear retained market data only. */
    CLEAR_MARKET_DATA(true),
    /** Purge runtime order/statistics state. */
    PURGE_RUNTIME_STATE(true),
    /** Keep the active policy while purging runtime state. */
    KEEP_POLICY_PURGE_RUNTIME(true),
    /** Purge state and repopulate with supplied scenario data. */
    PURGE_AND_REPOPULATE(true),
    /** Reset into a deterministic scenario replay state. */
    SCENARIO_REPLAY_RESET(true),
    /** Rebuild state from recovery inputs. */
    RECOVERY_REBUILD(true),
    /** Append new scenario data without destructive cleanup. */
    APPEND(false);

    private final boolean destructive;

    SorResetMode(final boolean destructive) {
        this.destructive = destructive;
    }

    /**
     * Reports whether the mode may destructively mutate existing engine state.
     *
     * @return true for destructive reset modes
     */
    public boolean destructive() {
        return destructive;
    }
}
