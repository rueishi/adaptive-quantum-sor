package com.nitroj.adaptive.quantum.sor.optimizer;

/**
 * Responsibility: carry strategic optimizer venue subset output.
 *
 * <p>Role in system: the L4 optimizer decides which venue IDs are structurally
 * eligible for each instrument/regime/urgency route key. Tactical optimization
 * and policy compilation then tune parameters inside that subset.</p>
 *
 * <p>Relationships: referenced by policy optimization input, tactical
 * optimization, and later policy compilation.</p>
 *
 * <p>Lifecycle: produced per strategic optimizer run and immutable by
 * convention after creation.</p>
 *
 * <p>Design intent: offset plus flattened venue arrays keep subset size
 * variable while retaining deterministic route-key lookup.</p>
 */
public final class StrategicVenueSubsetResult {
    public long version;
    public long createdAtNanos;

    public int instrumentCount;
    public int regimeCount;
    public int urgencyCount;

    public int[] subsetOffset;
    public short[] selectedVenueIds;

    public long optimizerRunId;
    public int optimizerType;

    public StrategicVenueSubsetResult() {
    }

    /**
     * Computes the canonical route key for the subset offset array.
     */
    public int routeKey(final int instrumentId, final int regimeId, final int urgencyId) {
        return ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId;
    }
}
