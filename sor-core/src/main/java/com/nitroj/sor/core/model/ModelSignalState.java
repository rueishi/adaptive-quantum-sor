package com.nitroj.sor.core.model;

import com.nitroj.sor.core.util.Indexing;

/**
 * Responsibility: store bounded model signals by instrument, venue, and regime.
 *
 * <p>Role in system: ML-style heuristics write this state and optimizers read
 * it as a deterministic score input.</p>
 *
 * <p>Relationships: produced by {@code MlSignalModelStub} from feature stats
 * and referenced by policy optimization input.</p>
 *
 * <p>Lifecycle: allocated for configured dimensions and replaced or updated
 * whenever the model version changes.</p>
 *
 * <p>Design intent: all signals use 0..10000 bps-like scale with 5000 as a
 * neutral insufficient-data value.</p>
 */
public final class ModelSignalState {
    private final Indexing indexing;
    private final int[] venueScoreBps;
    private long modelVersion;
    private long sequence;

    public ModelSignalState(final int instrumentCount, final int venueCount, final int regimeCount) {
        this.indexing = new Indexing(instrumentCount, venueCount, regimeCount, 1);
        this.venueScoreBps = new int[instrumentCount * venueCount * regimeCount];
        for (int i = 0; i < venueScoreBps.length; i++) {
            venueScoreBps[i] = 5_000;
        }
    }

    /** Sets a bounded model score for one IVR cell. */
    public void setVenueScoreBps(final int instrumentId, final int venueId, final int regimeId, final int scoreBps) {
        venueScoreBps[indexing.idxIVR(instrumentId, venueId, regimeId)] = Math.max(0, Math.min(10_000, scoreBps));
        sequence++;
    }

    public int venueScoreBps(final int instrumentId, final int venueId, final int regimeId) {
        return venueScoreBps[indexing.idxIVR(instrumentId, venueId, regimeId)];
    }

    public void setModelVersion(final long modelVersion) {
        if (modelVersion < 0) {
            throw new IllegalArgumentException("modelVersion must be non-negative");
        }
        this.modelVersion = modelVersion;
        sequence++;
    }

    public long modelVersion() {
        return modelVersion;
    }

    public long sequence() {
        return sequence;
    }
}
