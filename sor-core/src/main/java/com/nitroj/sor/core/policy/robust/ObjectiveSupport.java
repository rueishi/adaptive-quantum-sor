package com.nitroj.sor.core.policy.robust;

/**
 * Provides objective support support for robust policy objective and scenario-set selection models.
 *
 * <p>Use it from strategic optimization and robust scenario sweep evaluation; keep callers inside the owning core subsystem unless the type is intentionally public within sor-core.</p>
 */
final class ObjectiveSupport {
    private ObjectiveSupport() {
    }

    static void requireMatrix(final ScoreMatrix matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
    }

    static RobustSelection selection(
            final ScoreMatrix matrix,
            final double[] values,
            final RobustObjectiveType objective,
            final String parameters
    ) {
        int selectedIndex = 0;
        double best = values[0];
        final int[] candidateIds = matrix.candidateIds();
        for (int i = 1; i < values.length; i++) {
            if (values[i] > best || (Double.compare(values[i], best) == 0
                    && candidateIds[i] < candidateIds[selectedIndex])) {
                best = values[i];
                selectedIndex = i;
            }
        }
        return new RobustSelection(
                candidateIds[selectedIndex],
                values,
                objective,
                parameters,
                "lowest candidateId on equal objective value",
                ""
        );
    }
}
