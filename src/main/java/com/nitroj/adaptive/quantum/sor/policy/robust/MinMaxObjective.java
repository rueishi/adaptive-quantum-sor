package com.nitroj.adaptive.quantum.sor.policy.robust;

/**
 * Responsibility: select the highest worst-case scenario score.
 */
public final class MinMaxObjective implements RobustObjective {
    @Override
    public RobustSelection select(final ScoreMatrix matrix) {
        ObjectiveSupport.requireMatrix(matrix);
        final double[] values = new double[matrix.candidateCount()];
        for (int candidateIndex = 0; candidateIndex < matrix.candidateCount(); candidateIndex++) {
            long worst = Long.MAX_VALUE;
            for (int scenarioIndex = 0; scenarioIndex < matrix.scenarioCount(); scenarioIndex++) {
                worst = Math.min(worst, matrix.score(candidateIndex, scenarioIndex));
            }
            values[candidateIndex] = worst;
        }
        return ObjectiveSupport.selection(matrix, values, RobustObjectiveType.MIN_MAX, "min");
    }
}
