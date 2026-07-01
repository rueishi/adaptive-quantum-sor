package com.nitroj.sor.core.policy.robust;

/**
 * Responsibility: select the candidate with smallest maximum per-scenario regret.
 */
public final class MinRegretObjective implements RobustObjective {
    @Override
    public RobustSelection select(final ScoreMatrix matrix) {
        ObjectiveSupport.requireMatrix(matrix);
        final double[] values = new double[matrix.candidateCount()];
        for (int candidateIndex = 0; candidateIndex < matrix.candidateCount(); candidateIndex++) {
            long maxRegret = Long.MIN_VALUE;
            for (int scenarioIndex = 0; scenarioIndex < matrix.scenarioCount(); scenarioIndex++) {
                long best = Long.MIN_VALUE;
                for (int peer = 0; peer < matrix.candidateCount(); peer++) {
                    best = Math.max(best, matrix.score(peer, scenarioIndex));
                }
                maxRegret = Math.max(maxRegret, best - matrix.score(candidateIndex, scenarioIndex));
            }
            values[candidateIndex] = -maxRegret;
        }
        return ObjectiveSupport.selection(matrix, values, RobustObjectiveType.MIN_REGRET, "negativeMaxRegret");
    }
}
