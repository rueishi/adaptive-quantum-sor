package com.nitroj.sor.core.policy.robust;

/**
 * Responsibility: render operator-facing robust-selection explanation text.
 */
public final class RobustSelectionNarrative {
    private RobustSelectionNarrative() {
    }

    public static String render(final RobustSelectionProvenance provenance, final int selectedCandidateId) {
        if (provenance == null) {
            throw new IllegalArgumentException("provenance must not be null");
        }
        final StringBuilder out = new StringBuilder();
        out.append("Selected candidate ").append(selectedCandidateId)
                .append(" by ").append(provenance.robustObjective());
        if (!provenance.robustObjectiveParameters().isBlank()) {
            out.append(" (").append(provenance.robustObjectiveParameters()).append(")");
        }
        out.append(" over scenario set ").append(provenance.scenarioSetId())
                .append(" v").append(provenance.scenarioSetVersion())
                .append(" with ").append(provenance.scenarioCount()).append(" scenarios and ")
                .append(provenance.candidateCount()).append(" candidates.");
        if (!provenance.adequacyMissingCategories().isEmpty()) {
            out.append(" Missing adequacy categories: ")
                    .append(String.join(", ", provenance.adequacyMissingCategories()))
                    .append('.');
        }
        if (!provenance.scoreMatrixHandle().isBlank()) {
            out.append(" Score matrix: ").append(provenance.scoreMatrixHandle()).append('.');
        }
        return out.toString();
    }
}
