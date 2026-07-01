package com.nitroj.sor.api;

import java.util.Arrays;

/**
 * Responsibility: immutable evidence for a reset attempt.
 *
 * <p>Role in system: tells scenario runners, operators, and tests exactly
 * whether a reset was accepted and which state categories were cleared, kept,
 * or repopulated.</p>
 *
 * <p>Relationships: returned from {@link SorControlPlane#reset}.</p>
 *
 * <p>Lifecycle: created once per reset attempt and owned by the caller.</p>
 *
 * <p>Design intent: make purge behavior auditable instead of hidden behind a
 * side effect.</p>
 *
 * @param mode requested reset mode
 * @param accepted whether the reset mutated state
 * @param replaySafe whether resulting state is replay-safe
 * @param message operator-facing result message
 * @param clearedState state categories cleared
 * @param keptState state categories kept
 * @param repopulatedState state categories repopulated
 */
public record SorResetSummary(
        SorResetMode mode,
        boolean accepted,
        boolean replaySafe,
        String message,
        String[] clearedState,
        String[] keptState,
        String[] repopulatedState
) {
    /**
     * Validates and defensively copies reset evidence arrays.
     */
    public SorResetSummary {
        if (mode == null || message == null || message.isBlank()
                || clearedState == null || keptState == null || repopulatedState == null) {
            throw new IllegalArgumentException("reset summary inputs must not be null or blank");
        }
        clearedState = clearedState.clone();
        keptState = keptState.clone();
        repopulatedState = repopulatedState.clone();
    }

    /**
     * Returns a defensive copy of cleared state categories.
     *
     * @return cleared state categories
     */
    @Override
    public String[] clearedState() {
        return clearedState.clone();
    }

    /**
     * Returns a defensive copy of kept state categories.
     *
     * @return kept state categories
     */
    @Override
    public String[] keptState() {
        return keptState.clone();
    }

    /**
     * Returns a defensive copy of repopulated state categories.
     *
     * @return repopulated state categories
     */
    @Override
    public String[] repopulatedState() {
        return repopulatedState.clone();
    }

    @Override
    public String toString() {
        return "SorResetSummary{" + mode + ',' + accepted + ',' + replaySafe + ','
                + message + ',' + Arrays.toString(clearedState) + ','
                + Arrays.toString(keptState) + ',' + Arrays.toString(repopulatedState) + '}';
    }
}
