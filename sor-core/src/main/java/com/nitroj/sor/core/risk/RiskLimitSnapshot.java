package com.nitroj.sor.core.risk;

import com.nitroj.sor.core.util.Indexing;

/**
 * Responsibility: store static pre-trade risk limits for Phase 1.
 *
 * <p>Role in system: execution guards, policy linting, and policy compilation
 * need maximum child size, venue exposure, and participation limits before real
 * risk services exist.</p>
 *
 * <p>Relationships: shares dense instrument and venue IDs with metadata and fee
 * snapshots. Later policy linting will compare candidate limits against this
 * snapshot.</p>
 *
 * <p>Lifecycle: generated from config during startup and replaced atomically by
 * future risk simulator snapshots.</p>
 *
 * <p>Design intent: primitive arrays provide deterministic limits and explicit
 * validation for API/control-plane inputs before they enter engine queues.</p>
 */
public final class RiskLimitSnapshot {
    private final Indexing indexing;
    private final long[] maxChildQtyByInstrument;
    private final long[] maxVenueNotional;
    private final int[] maxParticipationBps;

    public RiskLimitSnapshot(final int instrumentCount, final int venueCount) {
        this.indexing = new Indexing(instrumentCount, venueCount, 1, 1);
        this.maxChildQtyByInstrument = new long[instrumentCount];
        this.maxVenueNotional = new long[indexing.ivLength()];
        this.maxParticipationBps = new int[indexing.ivLength()];
    }

    /**
     * Sets the maximum child quantity for an instrument.
     */
    public void setMaxChildQty(final int instrumentId, final long maxChildQty) {
        if (instrumentId < 0 || instrumentId >= maxChildQtyByInstrument.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
        if (maxChildQty <= 0) {
            throw new IllegalArgumentException("maxChildQty must be positive");
        }
        maxChildQtyByInstrument[instrumentId] = maxChildQty;
    }

    /**
     * Sets venue-specific notional and participation limits.
     */
    public void setVenueLimits(
            final int instrumentId,
            final int venueId,
            final long maxVenueNotional,
            final int maxParticipationBps
    ) {
        if (maxVenueNotional < 0) {
            throw new IllegalArgumentException("maxVenueNotional must be non-negative");
        }
        if (maxParticipationBps < 0 || maxParticipationBps > 10_000) {
            throw new IllegalArgumentException("maxParticipationBps must be in [0,10000]");
        }
        final int idx = indexing.idxIV(instrumentId, venueId);
        this.maxVenueNotional[idx] = maxVenueNotional;
        this.maxParticipationBps[idx] = maxParticipationBps;
    }

    public long maxChildQty(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= maxChildQtyByInstrument.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
        return maxChildQtyByInstrument[instrumentId];
    }

    public long maxVenueNotional(final int instrumentId, final int venueId) {
        return maxVenueNotional[indexing.idxIV(instrumentId, venueId)];
    }

    public int maxParticipationBps(final int instrumentId, final int venueId) {
        return maxParticipationBps[indexing.idxIV(instrumentId, venueId)];
    }
}
