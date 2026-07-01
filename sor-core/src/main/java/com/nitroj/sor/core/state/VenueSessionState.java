package com.nitroj.sor.core.state;

import com.nitroj.sor.core.model.VenueStatus;

/**
 * Responsibility: store venue session availability.
 *
 * <p>Role in system: policy compilation and execution guards use this state to
 * exclude closed, outage, or disabled venues.</p>
 *
 * <p>Relationships: venue session simulators write status values; execution and
 * metadata tests read {@link #isAvailable(int)}.</p>
 *
 * <p>Lifecycle: allocated once and updated when simulated venue state changes.</p>
 *
 * <p>Design intent: a single primitive array gives the hot path a cheap venue
 * availability check.</p>
 */
public final class VenueSessionState {
    private final int[] statusByVenue;

    public VenueSessionState(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.statusByVenue = new int[venueCount];
    }

    public void setStatus(final int venueId, final int status) {
        checkVenue(venueId);
        if (!VenueStatus.isValid(status)) {
            throw new IllegalArgumentException("status must be a known VenueStatus code");
        }
        statusByVenue[venueId] = status;
    }

    public int status(final int venueId) {
        checkVenue(venueId);
        return statusByVenue[venueId];
    }

    public boolean isAvailable(final int venueId) {
        return VenueStatus.isAvailable(status(venueId));
    }

    private void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= statusByVenue.length) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }
}
