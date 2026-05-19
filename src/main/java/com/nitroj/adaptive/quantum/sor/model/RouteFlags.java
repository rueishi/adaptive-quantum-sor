package com.nitroj.adaptive.quantum.sor.model;

/**
 * Responsibility: define bit flags that describe route and venue-order behavior.
 *
 * <p>Role in system: policy candidates, metadata capabilities, and child-order
 * records share this compact flag vocabulary. The flags are primitive shorts so
 * they can be stored in policy and child-order arrays without allocation.</p>
 *
 * <p>Relationships: {@code OrderTypeCapabilityMatrix} validates whether a venue
 * supports selected flags, and future policy linting checks will reject invalid
 * flag/capability combinations.</p>
 *
 * <p>Lifecycle: flag constants are stable for the process lifetime. New flags
 * should be added only when a task card owns the behavior.</p>
 *
 * <p>Design intent: bit flags are a simple production-style representation for
 * compact route policy state.</p>
 */
public final class RouteFlags {
    public static final short IOC = 1;
    public static final short POST_ONLY = 1 << 1;
    public static final short HIDDEN = 1 << 2;
    public static final short MIDPOINT = 1 << 3;
    public static final short LIT = 1 << 4;
    public static final short DARK = 1 << 5;
    public static final short ALL_KNOWN = IOC | POST_ONLY | HIDDEN | MIDPOINT | LIT | DARK;

    private RouteFlags() {
    }

    /**
     * Tests whether all bits in {@code flag} are present in {@code routeFlags}.
     *
     * @param routeFlags combined route flags
     * @param flag flag or flag mask to test
     * @return true when all requested bits are present
     */
    public static boolean has(final short routeFlags, final short flag) {
        return (routeFlags & flag) == flag;
    }

    /**
     * Adds a flag to an existing flag set.
     *
     * @param routeFlags existing flag set
     * @param flag flag to add
     * @return combined flag set
     */
    public static short add(final short routeFlags, final short flag) {
        return (short) (routeFlags | flag);
    }

    /**
     * Checks whether a route flag mask contains only known Phase 1 bits.
     *
     * @param routeFlags route flag bitmask
     * @return true when no unknown bits are set
     */
    public static boolean isKnownMask(final short routeFlags) {
        return (routeFlags & ~ALL_KNOWN) == 0;
    }
}
