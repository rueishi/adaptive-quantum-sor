package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import com.nitroj.sor.api.spi.RiskProvider;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Responsibility: deterministic synchronous simulator risk checks.
 *
 * <p>Role in system: implements the production {@link RiskProvider} SPI and
 * also exposes simulator-local risk snapshots matching legacy generated caps.</p>
 *
 * <p>Relationships: scenario runners call {@link #generate(SimConfig)} for
 * snapshot parity and the engine calls {@link #check(RiskCheckRequest,
 * RiskDecision)} on the hot path.</p>
 *
 * <p>Lifecycle: configured at scenario setup, read many times during routing,
 * and updated only through explicit control-plane methods.</p>
 *
 * <p>Design intent: keep check allocation-free while preserving legacy child
 * quantity, notional, and participation limit formulas.</p>
 */
public final class SimulatedRiskProvider implements RiskProvider {
    private final AtomicLong maxQuantity = new AtomicLong(Long.MAX_VALUE);
    private volatile SimRiskLimits snapshot;

    public void maxQuantity(final long quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be non-negative");
        }
        maxQuantity.set(quantity);
    }

    /** Generates legacy-equivalent dense risk limits and installs them. */
    public SimRiskLimits generate(final SimConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final SimRiskLimits generated = new SimRiskLimits(config);
        long smallestChildLimit = Long.MAX_VALUE;
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            final long childLimit = 10_000L + instrumentId * 100L;
            generated.setMaxChildQty(instrumentId, childLimit);
            smallestChildLimit = Math.min(smallestChildLimit, childLimit);
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                generated.setVenueLimits(instrumentId, venueId, 1_000_000L + venueId * 10_000L, 2_500);
            }
        }
        snapshot = generated;
        maxQuantity.set(smallestChildLimit);
        return generated;
    }

    /** Returns the latest generated snapshot, or null before generation. */
    public SimRiskLimits snapshot() {
        return snapshot;
    }

    @Override
    public void check(final RiskCheckRequest request, final RiskDecision decision) {
        if (request == null || decision == null) {
            throw new IllegalArgumentException("request and decision must not be null");
        }
        if (request.quantity() <= maxQuantity.get()) {
            decision.allow();
        } else {
            decision.reject(1);
        }
    }
}
