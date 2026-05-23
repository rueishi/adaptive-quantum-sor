package com.nitroj.adaptive.quantum.sor.stats;

/**
 * Responsibility: store the current detected market regime per instrument.
 *
 * <p>Role in system: feature/regime detection writes this state and policy SOR
 * execution reads the regime ID used to select the active route list.</p>
 *
 * <p>Relationships: produced by {@code RegimeDetector} from market data and
 * consumed by policy optimization input and future execution code.</p>
 *
 * <p>Lifecycle: allocated for configured instruments and updated on each
 * feature aggregation pass.</p>
 *
 * <p>Design intent: small integer constants keep Phase 1 regime detection
 * deterministic and easy to serialize.</p>
 */
public final class RegimeState {
    public static final int NORMAL = 0;
    public static final int VOLATILE = 1;
    public static final int THIN_BOOK = 2;

    private final int[] regimeByInstrument;

    public RegimeState(final int instrumentCount) {
        if (instrumentCount <= 0) {
            throw new IllegalArgumentException("instrumentCount must be positive");
        }
        this.regimeByInstrument = new int[instrumentCount];
    }

    /** Sets a known regime ID for one instrument. */
    public void setRegime(final int instrumentId, final int regimeId) {
        checkInstrument(instrumentId);
        if (!isValid(regimeId)) {
            throw new IllegalArgumentException("regimeId must be NORMAL, VOLATILE, or THIN_BOOK");
        }
        regimeByInstrument[instrumentId] = regimeId;
    }

    public int regime(final int instrumentId) {
        checkInstrument(instrumentId);
        return regimeByInstrument[instrumentId];
    }

    public static boolean isValid(final int regimeId) {
        return regimeId == NORMAL || regimeId == VOLATILE || regimeId == THIN_BOOK;
    }

    private void checkInstrument(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= regimeByInstrument.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
    }
}
