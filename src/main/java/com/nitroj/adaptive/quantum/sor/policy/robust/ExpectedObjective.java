package com.nitroj.adaptive.quantum.sor.policy.robust;

/**
 * Responsibility: select the highest mean scenario score.
 */
public final class ExpectedObjective implements RobustObjective {
    private final RobustObjectiveType type;
    private final String parameters;

    public ExpectedObjective() {
        this(RobustObjectiveType.EXPECTED, "mean");
    }

    public ExpectedObjective(final RobustObjectiveType type, final String parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    @Override
    public RobustSelection select(final ScoreMatrix matrix) {
        ObjectiveSupport.requireMatrix(matrix);
        final double[] values = new double[matrix.candidateCount()];
        for (int candidateIndex = 0; candidateIndex < matrix.candidateCount(); candidateIndex++) {
            long sum = 0L;
            for (int scenarioIndex = 0; scenarioIndex < matrix.scenarioCount(); scenarioIndex++) {
                sum += matrix.score(candidateIndex, scenarioIndex);
            }
            values[candidateIndex] = sum / (double) matrix.scenarioCount();
        }
        return ObjectiveSupport.selection(matrix, values, type, parameters);
    }
}
