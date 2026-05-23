package com.nitroj.adaptive.quantum.sor.metadata;

import com.nitroj.adaptive.quantum.sor.util.Indexing;

/**
 * Responsibility: store static venue metadata and instrument support.
 *
 * <p>Role in system: policy linting and compilation must know whether a venue
 * is globally enabled and whether it supports a given instrument.</p>
 *
 * <p>Relationships: {@link OrderTypeCapabilityMatrix} stores order-type support
 * for the same venue ID space, while fee and risk snapshots use instrument x
 * venue layout.</p>
 *
 * <p>Lifecycle: generated from config or deterministic simulators at startup
 * and treated as immutable during Phase 1 runs.</p>
 *
 * <p>Design intent: dense arrays make support checks fast and transparent for
 * later hot-path guards.</p>
 */
public final class VenueMetadata {
    private final Indexing indexing;
    private final String[] venueNames;
    private final boolean[] enabled;
    private final boolean[] supportsInstrument;

    public VenueMetadata(
            final int instrumentCount,
            final String[] venueNames,
            final boolean[] enabled,
            final boolean[] supportsInstrument
    ) {
        if (venueNames == null || enabled == null || venueNames.length == 0 || venueNames.length != enabled.length) {
            throw new IllegalArgumentException("venueNames and enabled must be non-empty arrays of equal length");
        }
        this.indexing = new Indexing(instrumentCount, venueNames.length, 1, 1);
        if (supportsInstrument == null || supportsInstrument.length != indexing.ivLength()) {
            throw new IllegalArgumentException("supportsInstrument length must equal instrumentCount * venueCount");
        }
        this.venueNames = venueNames.clone();
        this.enabled = enabled.clone();
        this.supportsInstrument = supportsInstrument.clone();
        for (int i = 0; i < this.venueNames.length; i++) {
            if (this.venueNames[i] == null || this.venueNames[i].isBlank()) {
                throw new IllegalArgumentException("venue name must not be blank at index " + i);
            }
        }
    }

    /**
     * Creates deterministic simulated venue metadata where every enabled venue
     * supports every simulated instrument.
     */
    public static VenueMetadata simulated(final int instrumentCount, final int venueCount) {
        final String[] names = new String[venueCount];
        final boolean[] enabled = new boolean[venueCount];
        final boolean[] supports = new boolean[instrumentCount * venueCount];
        for (int venueId = 0; venueId < venueCount; venueId++) {
            names[venueId] = "VENUE" + venueId;
            enabled[venueId] = true;
        }
        for (int i = 0; i < supports.length; i++) {
            supports[i] = true;
        }
        return new VenueMetadata(instrumentCount, names, enabled, supports);
    }

    public boolean isEnabled(final int venueId) {
        checkVenue(venueId);
        return enabled[venueId];
    }

    public boolean supportsInstrument(final int instrumentId, final int venueId) {
        return supportsInstrument[indexing.idxIV(instrumentId, venueId)];
    }

    public String venueName(final int venueId) {
        checkVenue(venueId);
        return venueNames[venueId];
    }

    public int venueCount() {
        return venueNames.length;
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= venueNames.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
