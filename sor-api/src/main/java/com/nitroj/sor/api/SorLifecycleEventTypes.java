package com.nitroj.sor.api;

/**
 * Responsibility: public numeric lifecycle/audit event identifiers.
 *
 * <p>Role in system: lets persistence adapters, replay tools, tests, and
 * operators interpret the primitive event envelope used by
 * {@code com.nitroj.sor.api.spi.Persistence}.</p>
 *
 * <p>Relationships: written by embedded engines and consumed by replay or
 * diagnostics code.</p>
 *
 * <p>Lifecycle: constants are stable once published.</p>
 *
 * <p>Design intent: keep audit evidence typed without adding strings or
 * allocation-heavy payloads to the current persistence SPI.</p>
 */
public final class SorLifecycleEventTypes {
    /** Reset request was accepted. */
    public static final long RESET_ACCEPTED = 9_001L;
    /** Reset request was rejected. */
    public static final long RESET_REJECTED = 9_002L;
    /** Policy was published. */
    public static final long POLICY_PUBLISHED = 9_003L;
    /** Route decision was made. */
    public static final long ROUTE_DECIDED = 9_004L;
    /** Child order was emitted. */
    public static final long CHILD_ORDER_EMITTED = 9_005L;
    /** Venue fill was delivered. */
    public static final long FILL_DELIVERED = 9_006L;
    /** Venue reject was delivered. */
    public static final long REJECT_DELIVERED = 9_007L;
    /** Market data update was rejected. */
    public static final long MARKET_DATA_REJECTED = 9_008L;

    private SorLifecycleEventTypes() {
    }
}
