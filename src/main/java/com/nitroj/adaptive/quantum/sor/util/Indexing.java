package com.nitroj.adaptive.quantum.sor.util;

/**
 * Responsibility: implement the canonical dense-array index formulas.
 *
 * <p>Role in system: almost every Adaptive Quantum SOR data structure is backed by primitive
 * arrays. This utility centralizes the formulas mandated by the spec so state,
 * policy, optimizer, and tests do not drift into incompatible layouts.</p>
 *
 * <p>Relationships: market state, venue stats, metadata, policy candidates, and
 * hot route books use these formulas directly or mirror them exactly.</p>
 *
 * <p>Lifecycle: an {@link Indexing} instance is created for a fixed dimension
 * set. It is immutable and safe to share across warm-path and test code.</p>
 *
 * <p>Design intent: dense IDs are a Phase 1 constraint. Validating boundaries in
 * this utility catches configuration and caller errors before they corrupt array
 * state.</p>
 */
public final class Indexing {
    private final int instrumentCount;
    private final int venueCount;
    private final int regimeCount;
    private final int urgencyCount;

    /**
     * Creates an indexing helper for fixed dense dimensions.
     *
     * @param instrumentCount number of dense instrument IDs
     * @param venueCount number of dense venue IDs
     * @param regimeCount number of dense regime IDs
     * @param urgencyCount number of dense urgency IDs
     */
    public Indexing(
            final int instrumentCount,
            final int venueCount,
            final int regimeCount,
            final int urgencyCount
    ) {
        this.instrumentCount = requirePositive("instrumentCount", instrumentCount);
        this.venueCount = requirePositive("venueCount", venueCount);
        this.regimeCount = requirePositive("regimeCount", regimeCount);
        this.urgencyCount = requirePositive("urgencyCount", urgencyCount);
    }

    /**
     * Computes instrument x venue index.
     *
     * @param instrumentId dense instrument ID
     * @param venueId dense venue ID
     * @return {@code instrumentId * venueCount + venueId}
     */
    public int idxIV(final int instrumentId, final int venueId) {
        checkInstrument(instrumentId);
        checkVenue(venueId);
        return instrumentId * venueCount + venueId;
    }

    /**
     * Computes instrument x venue x regime index.
     *
     * @param instrumentId dense instrument ID
     * @param venueId dense venue ID
     * @param regimeId dense regime ID
     * @return canonical IVR index
     */
    public int idxIVR(final int instrumentId, final int venueId, final int regimeId) {
        checkRegime(regimeId);
        return idxIV(instrumentId, venueId) * regimeCount + regimeId;
    }

    /**
     * Computes instrument x venue x regime x urgency index.
     *
     * @param instrumentId dense instrument ID
     * @param venueId dense venue ID
     * @param regimeId dense regime ID
     * @param urgencyId dense urgency ID
     * @return canonical IVRU index
     */
    public int idxIVRU(
            final int instrumentId,
            final int venueId,
            final int regimeId,
            final int urgencyId
    ) {
        checkUrgency(urgencyId);
        return idxIVR(instrumentId, venueId, regimeId) * urgencyCount + urgencyId;
    }

    /**
     * Computes hot route key for instrument x regime x urgency.
     *
     * @param instrumentId dense instrument ID
     * @param regimeId dense regime ID
     * @param urgencyId dense urgency ID
     * @return canonical route key
     */
    public int routeKey(final int instrumentId, final int regimeId, final int urgencyId) {
        checkInstrument(instrumentId);
        checkRegime(regimeId);
        checkUrgency(urgencyId);
        return ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId;
    }

    /**
     * Computes the required HotRouteBook offset-array length.
     *
     * @return {@code instrumentCount * regimeCount * urgencyCount + 1}
     */
    public int routeListOffsetLength() {
        return instrumentCount * regimeCount * urgencyCount + 1;
    }

    /**
     * Computes total entries for IVRU arrays.
     *
     * @return {@code instrumentCount * venueCount * regimeCount * urgencyCount}
     */
    public int ivruLength() {
        return instrumentCount * venueCount * regimeCount * urgencyCount;
    }

    /**
     * Computes total entries for IV arrays.
     *
     * @return {@code instrumentCount * venueCount}
     */
    public int ivLength() {
        return instrumentCount * venueCount;
    }

    public int instrumentCount() {
        return instrumentCount;
    }

    public int venueCount() {
        return venueCount;
    }

    public int regimeCount() {
        return regimeCount;
    }

    public int urgencyCount() {
        return urgencyCount;
    }

    private static int requirePositive(final String name, final int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private void checkInstrument(final int instrumentId) {
        checkRange("instrumentId", instrumentId, instrumentCount);
    }

    private void checkVenue(final int venueId) {
        checkRange("venueId", venueId, venueCount);
    }

    private void checkRegime(final int regimeId) {
        checkRange("regimeId", regimeId, regimeCount);
    }

    private void checkUrgency(final int urgencyId) {
        checkRange("urgencyId", urgencyId, urgencyCount);
    }

    private static void checkRange(final String name, final int value, final int upperExclusive) {
        if (value < 0 || value >= upperExclusive) {
            throw new IndexOutOfBoundsException(
                    name + " out of range: " + value + " not in [0," + upperExclusive + ")"
            );
        }
    }
}
