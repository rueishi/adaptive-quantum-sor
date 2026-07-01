package com.nitroj.sor.testkit.sim.scenario.venues;

import com.nitroj.sor.api.VenueStatus;

import java.util.Arrays;

/** Public simulator snapshot of venue session status by venue. */
public final class SimVenueSessionSnapshot {
    private final VenueStatus[] statusByVenue;

    public SimVenueSessionSnapshot(final int venueCount) {
        this.statusByVenue = new VenueStatus[venueCount];
        Arrays.fill(statusByVenue, VenueStatus.CLOSED);
    }

    public void setStatus(final int venueId, final VenueStatus status) {
        statusByVenue[venueId] = status;
    }

    public VenueStatus status(final int venueId) {
        return statusByVenue[venueId];
    }

    public boolean isAvailable(final int venueId) {
        return statusByVenue[venueId] == VenueStatus.OPEN;
    }
}
