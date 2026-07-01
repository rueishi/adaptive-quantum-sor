package com.nitroj.sor.core.model;

/**
 * Responsibility: define primitive venue availability status codes.
 *
 * <p>Role in system: venue session and health state use these values so routing
 * guards can quickly skip unavailable venues.</p>
 *
 * <p>Relationships: {@code VenueSessionState} stores one status per venue, and
 * later policy compiler / executioner code uses {@link #isAvailable(int)} for
 * simple live guards.</p>
 *
 * <p>Lifecycle: constants are immutable and valid for the whole process.</p>
 *
 * <p>Design intent: the Adaptive Quantum SOR needs a clear distinction between open, closed,
 * outage, and administratively disabled venues without modeling real exchange
 * sessions yet.</p>
 */
public final class VenueStatus {
    public static final int OPEN = 0;
    public static final int CLOSED = 1;
    public static final int OUTAGE = 2;
    public static final int DISABLED = 3;

    private VenueStatus() {
    }

    /**
     * Determines whether a venue status can receive routed child orders.
     *
     * @param status primitive venue status code
     * @return true only for {@link #OPEN}
     */
    public static boolean isAvailable(final int status) {
        return status == OPEN;
    }

    /**
     * Checks whether a venue status code is one of the supported constants.
     *
     * @param status primitive venue status code
     * @return true when the code is known
     */
    public static boolean isValid(final int status) {
        return status == OPEN || status == CLOSED || status == OUTAGE || status == DISABLED;
    }
}
