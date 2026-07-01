package com.nitroj.sor.testkit.sim.scenario;

/**
 * Responsibility: stable simulator regime identifiers.
 *
 * <p>Role in system: mirrors the legacy normal, volatile, and thin-book
 * integer regimes while remaining local to {@code sor-test-server}.</p>
 *
 * <p>Relationships: market data, sessions, throttles, venue outcomes, parent
 * order generation, and scenario replay branch on these constants.</p>
 *
 * <p>Lifecycle: constants are fixed for deterministic scenario compatibility.</p>
 *
 * <p>Design intent: keep public simulator APIs primitive-friendly while giving
 * tests and docs named values for the legacy-compatible regimes.</p>
 */
public final class SimRegime {
    public static final int NORMAL = 0;
    public static final int VOLATILE = 1;
    public static final int THIN_BOOK = 2;

    private SimRegime() {
    }

    /** Returns true when the supplied regime ID is one of the supported values. */
    public static boolean isValid(final int regimeId) {
        return regimeId == NORMAL || regimeId == VOLATILE || regimeId == THIN_BOOK;
    }
}
