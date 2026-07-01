package com.nitroj.sor.core.policy.robust;

import java.util.List;

/**
 * Responsibility: immutable governance provenance for one robust publication.
 */
public record RobustSelectionProvenance(
        String robustObjective,
        String robustObjectiveParameters,
        String scenarioSetId,
        long scenarioSetVersion,
        int scenarioCount,
        int candidateCount,
        String scoreMatrixHandle,
        String adequacyStatus,
        List<String> adequacyMissingCategories
) {
    public RobustSelectionProvenance {
        robustObjective = robustObjective == null ? "" : robustObjective;
        robustObjectiveParameters = robustObjectiveParameters == null ? "" : robustObjectiveParameters;
        scenarioSetId = scenarioSetId == null ? "" : scenarioSetId;
        scoreMatrixHandle = scoreMatrixHandle == null ? "" : scoreMatrixHandle;
        adequacyStatus = adequacyStatus == null ? "" : adequacyStatus;
        adequacyMissingCategories = adequacyMissingCategories == null ? List.of() : List.copyOf(adequacyMissingCategories);
        if (scenarioSetVersion < 0 || scenarioCount < 0 || candidateCount < 0) {
            throw new IllegalArgumentException("provenance counts and versions must be non-negative");
        }
    }
}
