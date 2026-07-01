package com.nitroj.sor.testkit.sim.scenario.venues;

/** Public simulator venue event emitted by deterministic outcome processing. */
public record SimVenueOutcome(long childOrderId, long parentOrderId, int venueId, String type,
                              long quantity, long latencyNanos, int slippageBps, int toxicityBps) {
    public SimVenueOutcome {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
    }
}
