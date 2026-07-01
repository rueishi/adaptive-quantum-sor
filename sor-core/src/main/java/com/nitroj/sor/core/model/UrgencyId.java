package com.nitroj.sor.core.model;

/**
 * Responsibility: define dense urgency IDs for parent-order routing behavior.
 *
 * <p>Role in system: urgency participates in the hot route key and allows later
 * policy compilation to choose different route lists for passive versus urgent
 * execution.</p>
 *
 * <p>Relationships: {@code OrderIntent} stores one of these IDs and the
 * canonical indexing utility uses urgency as the final route-key dimension.</p>
 *
 * <p>Lifecycle: constants are immutable and valid for the full process life.</p>
 *
 * <p>Design intent: named integer constants are enough for Phase 1 and keep the
 * hot path independent from enum dispatch.</p>
 */
public final class UrgencyId {
    public static final int LOW = 0;
    public static final int NORMAL = 1;
    public static final int HIGH = 2;
    public static final int IMMEDIATE = 3;

    private UrgencyId() {
    }
}
