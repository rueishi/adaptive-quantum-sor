package com.nitroj.sor.api;

/**
 * Responsibility: public side constants for parent and child order direction.
 *
 * <p>Role in system: integrators use these primitive values in
 * {@link ParentOrderRequest}; the engine can translate them to internal model
 * enums without exposing those internals.</p>
 *
 * <p>Relationships: used by request DTOs and event records that carry side as a
 * hot-path primitive.</p>
 *
 * <p>Lifecycle: constants are loaded with the API jar and never instantiated.</p>
 *
 * <p>Design intent: primitive constants avoid enum allocation and serialization
 * ambiguity on hot API boundaries.</p>
 */
public final class Side {
    public static final int BUY = 1;
    public static final int SELL = 2;

    private Side() {
    }

    /**
     * Returns true when the supplied side is one of the public API constants.
     *
     * @param side side code to validate
     * @return true for {@link #BUY} or {@link #SELL}
     */
    public static boolean isValid(final int side) {
        return side == BUY || side == SELL;
    }
}
