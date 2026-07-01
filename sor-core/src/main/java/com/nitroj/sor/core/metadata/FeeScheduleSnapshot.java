package com.nitroj.sor.core.metadata;

import com.nitroj.sor.core.util.Indexing;

/**
 * Responsibility: store simulated maker/taker fees by instrument and venue.
 *
 * <p>Role in system: static SOR, policy optimization, and policy compilation
 * need deterministic fee penalties before real venue fee files exist.</p>
 *
 * <p>Relationships: shares IV indexing with venue metadata and risk snapshots.</p>
 *
 * <p>Lifecycle: generated from config or simulator at startup and read as a
 * snapshot until replaced by a later config task.</p>
 *
 * <p>Design intent: fees are stored in ticks to avoid floating-point behavior in
 * deterministic route ranking.</p>
 */
public final class FeeScheduleSnapshot {
    private final Indexing indexing;
    private final int[] makerFeeTicks;
    private final int[] takerFeeTicks;

    public FeeScheduleSnapshot(final int instrumentCount, final int venueCount) {
        this.indexing = new Indexing(instrumentCount, venueCount, 1, 1);
        this.makerFeeTicks = new int[indexing.ivLength()];
        this.takerFeeTicks = new int[indexing.ivLength()];
    }

    /**
     * Updates fee ticks for one instrument/venue pair.
     */
    public void setFees(final int instrumentId, final int venueId, final int makerFeeTicks, final int takerFeeTicks) {
        final int idx = indexing.idxIV(instrumentId, venueId);
        this.makerFeeTicks[idx] = makerFeeTicks;
        this.takerFeeTicks[idx] = takerFeeTicks;
    }

    public int makerFeeTicks(final int instrumentId, final int venueId) {
        return makerFeeTicks[indexing.idxIV(instrumentId, venueId)];
    }

    public int takerFeeTicks(final int instrumentId, final int venueId) {
        return takerFeeTicks[indexing.idxIV(instrumentId, venueId)];
    }
}
