package com.nitroj.adaptive.quantum.sor.policy.robust;

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
