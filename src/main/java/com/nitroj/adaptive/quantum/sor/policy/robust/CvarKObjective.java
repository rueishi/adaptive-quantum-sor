package com.nitroj.adaptive.quantum.sor.policy.robust;

import java.util.Arrays;

/**
 * Responsibility: select the highest mean of the worst k percent of scores.
 */
public final class CvarKObjective implements RobustObjective {
    private final int kPercent;

    public CvarKObjective(final int kPercent) {
        if (kPercent < 1 || kPercent > 100) {
            throw new IllegalArgumentException("kPercent must be in 1..100");
        }
        this.kPercent = kPercent;
    }

    @Override
    public RobustSelection select(final ScoreMatrix matrix) {
        ObjectiveSupport.requireMatrix(matrix);
        final double[] values = new double[matrix.candidateCount()];
        final int tailCount = Math.max(1, (int) Math.ceil(matrix.scenarioCount() * (kPercent / 100.0d)));
        for (int candidateIndex = 0; candidateIndex < matrix.candidateCount(); candidateIndex++) {
            final long[] candidateScores = matrix.scores()[candidateIndex];
            Arrays.sort(candidateScores);
            long sum = 0L;
            for (int i = 0; i < tailCount; i++) {
                sum += candidateScores[i];
            }
            values[candidateIndex] = sum / (double) tailCount;
        }
        return ObjectiveSupport.selection(matrix, values, RobustObjectiveType.CVAR_K, "k=" + kPercent);
    }
}
