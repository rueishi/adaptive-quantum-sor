package com.nitroj.sor.core.policy.robust;

/**
 * Responsibility: carry the deterministic robust-selection result.
 */
public record RobustSelection(
        int selectedCandidateId,
        double[] objectiveValues,
        RobustObjectiveType objective,
        String objectiveParameters,
        String tieBreakBasis,
        String scoreMatrixHandle
) {
    public RobustSelection {
        if (selectedCandidateId < 0) {
            throw new IllegalArgumentException("selectedCandidateId must be non-negative");
        }
        if (objectiveValues == null || objectiveValues.length == 0) {
            throw new IllegalArgumentException("objectiveValues must not be empty");
        }
        objectiveValues = objectiveValues.clone();
        if (objective == null) {
            throw new IllegalArgumentException("objective must not be null");
        }
        objectiveParameters = objectiveParameters == null ? "" : objectiveParameters;
        tieBreakBasis = tieBreakBasis == null ? "" : tieBreakBasis;
        scoreMatrixHandle = scoreMatrixHandle == null ? "" : scoreMatrixHandle;
    }

    @Override
    public double[] objectiveValues() {
        return objectiveValues.clone();
    }

    public RobustSelection withScoreMatrixHandle(final String handle) {
        return new RobustSelection(selectedCandidateId, objectiveValues, objective, objectiveParameters, tieBreakBasis, handle);
    }
}
