package com.nitroj.sor.core.model;

/**
 * Responsibility: define dense side constants for parent and child orders.
 *
 * <p>Role in system: Phase 1 avoids enum allocation and object identity in
 * hot-path structures by using primitive integer constants. Routing and market
 * state code can branch on these stable values without depending on external
 * trading libraries.</p>
 *
 * <p>Relationships: {@code OrderIntent}, {@code ChildOrder}, and future router
 * logic use these constants to determine buy-side versus sell-side behavior.</p>
 *
 * <p>Lifecycle: constants are loaded once with the class and never change.</p>
 *
 * <p>Design intent: dense primitive IDs keep array-backed data structures simple
 * and deterministic for the Adaptive Quantum SOR.</p>
 */
public final class Side {
    public static final int BUY = 1;
    public static final int SELL = 2;

    private Side() {
    }

    /**
     * Checks whether a side code is one of the supported dense constants.
     *
     * @param side side code to validate
     * @return true when the code is {@link #BUY} or {@link #SELL}
     */
    public static boolean isValid(final int side) {
        return side == BUY || side == SELL;
    }

    /**
     * Parses a control-plane side value into the dense side constant.
     *
     * @param value side text such as {@code BUY}, {@code SELL}, {@code 1}, or {@code 2}
     * @return dense side constant
     */
    public static int parse(final String value) {
        if ("BUY".equalsIgnoreCase(value) || "1".equals(value)) {
            return BUY;
        }
        if ("SELL".equalsIgnoreCase(value) || "2".equals(value)) {
            return SELL;
        }
        throw new IllegalArgumentException("side must be BUY or SELL");
    }
}
