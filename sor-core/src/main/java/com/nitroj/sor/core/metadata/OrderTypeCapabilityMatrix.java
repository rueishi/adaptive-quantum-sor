package com.nitroj.sor.core.metadata;

import com.nitroj.sor.core.model.RouteFlags;

/**
 * Responsibility: store venue order-type capabilities.
 *
 * <p>Role in system: policy linting checks route flags against this matrix so a
 * candidate cannot enable midpoint or hidden behavior on unsupported venues.</p>
 *
 * <p>Relationships: {@link VenueMetadata} validates venue existence and
 * instrument support, while this class validates flag support within a venue.</p>
 *
 * <p>Lifecycle: generated at startup from simulated venue metadata and read as a
 * static capability snapshot.</p>
 *
 * <p>Design intent: separate boolean arrays make each capability explicit and
 * easy to test before richer venue metadata is added.</p>
 */
public final class OrderTypeCapabilityMatrix {
    private final boolean[] supportsIoc;
    private final boolean[] supportsPostOnly;
    private final boolean[] supportsHidden;
    private final boolean[] supportsMidpoint;
    private final boolean[] supportsLit;
    private final boolean[] supportsDark;

    public OrderTypeCapabilityMatrix(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.supportsIoc = new boolean[venueCount];
        this.supportsPostOnly = new boolean[venueCount];
        this.supportsHidden = new boolean[venueCount];
        this.supportsMidpoint = new boolean[venueCount];
        this.supportsLit = new boolean[venueCount];
        this.supportsDark = new boolean[venueCount];
    }

    /**
     * Creates permissive deterministic capabilities for simulated venues.
     */
    public static OrderTypeCapabilityMatrix simulated(final int venueCount) {
        final OrderTypeCapabilityMatrix matrix = new OrderTypeCapabilityMatrix(venueCount);
        for (int venueId = 0; venueId < venueCount; venueId++) {
            matrix.setCapabilities(venueId, true, true, true, true, true, true);
        }
        return matrix;
    }

    /**
     * Sets all supported order-type capabilities for a venue.
     */
    public void setCapabilities(
            final int venueId,
            final boolean ioc,
            final boolean postOnly,
            final boolean hidden,
            final boolean midpoint,
            final boolean lit,
            final boolean dark
    ) {
        checkVenue(venueId);
        supportsIoc[venueId] = ioc;
        supportsPostOnly[venueId] = postOnly;
        supportsHidden[venueId] = hidden;
        supportsMidpoint[venueId] = midpoint;
        supportsLit[venueId] = lit;
        supportsDark[venueId] = dark;
    }

    /**
     * Checks whether a venue supports every requested route flag.
     *
     * @param venueId dense venue ID
     * @param flags route flag bitmask
     * @return true when all requested flag capabilities are available
     */
    public boolean supportsFlags(final int venueId, final short flags) {
        checkVenue(venueId);
        if (!RouteFlags.isKnownMask(flags)) {
            throw new IllegalArgumentException("flags contain unknown route bits: " + flags);
        }
        return (!RouteFlags.has(flags, RouteFlags.IOC) || supportsIoc[venueId])
                && (!RouteFlags.has(flags, RouteFlags.POST_ONLY) || supportsPostOnly[venueId])
                && (!RouteFlags.has(flags, RouteFlags.HIDDEN) || supportsHidden[venueId])
                && (!RouteFlags.has(flags, RouteFlags.MIDPOINT) || supportsMidpoint[venueId])
                && (!RouteFlags.has(flags, RouteFlags.LIT) || supportsLit[venueId])
                && (!RouteFlags.has(flags, RouteFlags.DARK) || supportsDark[venueId]);
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= supportsIoc.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
