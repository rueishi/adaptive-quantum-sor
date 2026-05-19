package com.nitroj.adaptive.quantum.sor.lifecycle;

/**
 * Responsibility: define stable lifecycle event type IDs.
 *
 * <p>Role in system: simulators, optimizers, policy components, execution, API,
 * and metrics can publish compact typed events to a narrative stream.</p>
 *
 * <p>Relationships: used by {@link LifecycleEvent} and
 * {@link NarrativeLifecycleLogger} implementations.</p>
 *
 * <p>Lifecycle: constants are process-wide and stable for Phase 1.</p>
 *
 * <p>Design intent: integer IDs avoid coupling warm-path components to
 * human-readable strings.</p>
 */
public final class LifecycleEventType {
    public static final int SIM_MARKET_DATA = 1;
    public static final int SIM_PARENT_ORDER = 2;
    public static final int SIM_VENUE_BEHAVIOR = 3;
    public static final int FEATURE_STATS_CHANGE = 4;
    public static final int ML_SIGNAL_CHANGE = 5;
    public static final int POLICY_COMPILED = 6;
    public static final int POLICY_PUBLISHED = 7;
    public static final int POLICY_REJECTED = 8;
    public static final int SOR_DECISION = 9;
    public static final int VENUE_OUTCOME = 10;
    public static final int AUDIT_EVENT = 11;
    public static final int METRICS_SUMMARY = 12;
    public static final int SIMULATOR_FAILURE = 13;
    public static final int SCENARIO_RESET = 14;
    public static final int SCENARIO_RUN = 15;

    private LifecycleEventType() {
    }
}
