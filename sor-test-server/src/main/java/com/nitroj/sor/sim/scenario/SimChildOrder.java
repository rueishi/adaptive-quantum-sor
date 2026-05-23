package com.nitroj.sor.sim.scenario;

/** Public simulator child order input used by deterministic venue outcome processing. */
public record SimChildOrder(long childOrderId, long parentOrderId, int instrumentId, int venueId,
                            int side, long quantity, long limitPrice, long createdEpochNanos) {
    public SimChildOrder {
        if (childOrderId <= 0 || parentOrderId <= 0) {
            throw new IllegalArgumentException("order ids must be positive");
        }
        if (instrumentId < 0 || venueId < 0) {
            throw new IllegalArgumentException("instrumentId and venueId must be non-negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
