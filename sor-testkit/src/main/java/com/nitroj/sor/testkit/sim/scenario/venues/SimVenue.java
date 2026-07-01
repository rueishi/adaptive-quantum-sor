package com.nitroj.sor.testkit.sim.scenario.venues;

/** Simulator-local venue metadata row. */
public record SimVenue(int venueId, String code, boolean enabled, boolean[] supportedInstruments) {
    public SimVenue {
        if (venueId < 0) {
            throw new IllegalArgumentException("venueId must be non-negative");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (supportedInstruments == null || supportedInstruments.length == 0) {
            throw new IllegalArgumentException("supportedInstruments must not be empty");
        }
        supportedInstruments = supportedInstruments.clone();
    }

    public boolean supports(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= supportedInstruments.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
        return supportedInstruments[instrumentId];
    }

    @Override
    public boolean[] supportedInstruments() {
        return supportedInstruments.clone();
    }
}
