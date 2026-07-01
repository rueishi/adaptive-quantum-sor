package com.nitroj.sor.core.policy.robust;

import java.util.Arrays;
import java.util.List;

/**
 * Responsibility: immutable candidate x scenario score evidence for Phase 7.
 */
public record ScoreMatrix(
        String matrixId,
        String scenarioSetId,
        long scenarioSetVersion,
        String scorecardVersion,
        int[] candidateIds,
        long[] candidatePolicyHash64,
        String[] candidatePolicyHashSha256Hex,
        String[] scenarioIds,
        String[] scenarioCategories,
        long[][] scores
) {
    public ScoreMatrix {
        if (matrixId == null || matrixId.isBlank()) {
            throw new IllegalArgumentException("matrixId must not be blank");
        }
        if (scenarioSetId == null || scenarioSetId.isBlank()) {
            throw new IllegalArgumentException("scenarioSetId must not be blank");
        }
        if (scenarioSetVersion <= 0) {
            throw new IllegalArgumentException("scenarioSetVersion must be positive");
        }
        if (scorecardVersion == null || scorecardVersion.isBlank()) {
            throw new IllegalArgumentException("scorecardVersion must not be blank");
        }
        candidateIds = candidateIds == null ? new int[0] : candidateIds.clone();
        candidatePolicyHash64 = candidatePolicyHash64 == null ? new long[0] : candidatePolicyHash64.clone();
        candidatePolicyHashSha256Hex = candidatePolicyHashSha256Hex == null
                ? new String[0]
                : candidatePolicyHashSha256Hex.clone();
        scenarioIds = scenarioIds == null ? new String[0] : scenarioIds.clone();
        scenarioCategories = scenarioCategories == null ? new String[0] : scenarioCategories.clone();
        if (candidateIds.length == 0 || scenarioIds.length == 0) {
            throw new IllegalArgumentException("matrix axes must not be empty");
        }
        if (candidatePolicyHash64.length != candidateIds.length
                || candidatePolicyHashSha256Hex.length != candidateIds.length) {
            throw new IllegalArgumentException("candidate metadata lengths must match");
        }
        if (scenarioCategories.length != scenarioIds.length) {
            throw new IllegalArgumentException("scenario metadata lengths must match");
        }
        scores = copyScores(scores, candidateIds.length, scenarioIds.length);
    }

    public static ScoreMatrix of(
            final String scenarioSetId,
            final long scenarioSetVersion,
            final String scorecardVersion,
            final List<PolicyCandidate> candidates,
            final ScenarioSetDescriptor scenarios,
            final long[][] scores
    ) {
        final int[] candidateIds = new int[candidates.size()];
        final long[] hash64 = new long[candidates.size()];
        final String[] sha = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            candidateIds[i] = candidates.get(i).candidateId();
            hash64[i] = candidates.get(i).canonicalPolicyHash64();
            sha[i] = candidates.get(i).canonicalPolicyHashSha256Hex();
        }
        final String[] scenarioIds = new String[scenarios.size()];
        final String[] categories = new String[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            scenarioIds[i] = scenarios.scenarioId(i);
            categories[i] = scenarios.category(i);
        }
        final String matrixId = scenarioSetId + "-v" + scenarioSetVersion + "-"
                + Integer.toUnsignedString(Arrays.hashCode(candidateIds), 16) + "-"
                + Integer.toUnsignedString(Arrays.hashCode(hash64), 16) + "-"
                + Integer.toUnsignedString(Arrays.hashCode(scenarioIds), 16) + "-"
                + Integer.toUnsignedString(Arrays.deepHashCode(scores), 16);
        return new ScoreMatrix(matrixId, scenarioSetId, scenarioSetVersion, scorecardVersion,
                candidateIds, hash64, sha, scenarioIds, categories, scores);
    }

    public int candidateCount() {
        return candidateIds.length;
    }

    public int scenarioCount() {
        return scenarioIds.length;
    }

    public long score(final int candidateIndex, final int scenarioIndex) {
        return scores[candidateIndex][scenarioIndex];
    }

    @Override
    public int[] candidateIds() {
        return candidateIds.clone();
    }

    @Override
    public long[] candidatePolicyHash64() {
        return candidatePolicyHash64.clone();
    }

    @Override
    public String[] candidatePolicyHashSha256Hex() {
        return candidatePolicyHashSha256Hex.clone();
    }

    @Override
    public String[] scenarioIds() {
        return scenarioIds.clone();
    }

    @Override
    public String[] scenarioCategories() {
        return scenarioCategories.clone();
    }

    @Override
    public long[][] scores() {
        return copyScores(scores, candidateIds.length, scenarioIds.length);
    }

    private static long[][] copyScores(final long[][] values, final int candidates, final int scenarios) {
        if (values == null || values.length != candidates) {
            throw new IllegalArgumentException("score rows must match candidates");
        }
        final long[][] copy = new long[candidates][scenarios];
        for (int i = 0; i < candidates; i++) {
            if (values[i] == null || values[i].length != scenarios) {
                throw new IllegalArgumentException("score columns must match scenarios");
            }
            copy[i] = values[i].clone();
        }
        return copy;
    }
}
