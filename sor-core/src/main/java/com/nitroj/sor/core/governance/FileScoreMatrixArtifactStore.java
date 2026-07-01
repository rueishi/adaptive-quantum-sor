package com.nitroj.sor.core.governance;

import com.nitroj.sor.core.policy.robust.ScoreMatrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsibility: write Phase 7 score matrices as deterministic CSV artifacts.
 */
public final class FileScoreMatrixArtifactStore implements ScoreMatrixArtifactStore {
    private final Path directory;

    public FileScoreMatrixArtifactStore(final Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("directory must not be null");
        }
        this.directory = directory;
    }

    @Override
    public String store(final ScoreMatrix matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix must not be null");
        }
        try {
            Files.createDirectories(directory);
            final Path output = directory.resolve(matrix.matrixId() + ".csv");
            Files.writeString(output, csv(matrix), StandardCharsets.UTF_8);
            return output.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to store score matrix", ex);
        }
    }

    private static String csv(final ScoreMatrix matrix) {
        final StringBuilder out = new StringBuilder();
        out.append("matrixId,scenarioSetId,scenarioSetVersion,scorecardVersion,candidateId,")
                .append("candidatePolicyHash64,candidatePolicyHashSha256Hex,scenarioId,scenarioCategory,score\n");
        final int[] candidateIds = matrix.candidateIds();
        final long[] hash64 = matrix.candidatePolicyHash64();
        final String[] sha = matrix.candidatePolicyHashSha256Hex();
        final String[] scenarioIds = matrix.scenarioIds();
        final String[] categories = matrix.scenarioCategories();
        for (int candidateIndex = 0; candidateIndex < matrix.candidateCount(); candidateIndex++) {
            for (int scenarioIndex = 0; scenarioIndex < matrix.scenarioCount(); scenarioIndex++) {
                out.append(matrix.matrixId()).append(',')
                        .append(matrix.scenarioSetId()).append(',')
                        .append(matrix.scenarioSetVersion()).append(',')
                        .append(matrix.scorecardVersion()).append(',')
                        .append(candidateIds[candidateIndex]).append(',')
                        .append(hash64[candidateIndex]).append(',')
                        .append(sha[candidateIndex]).append(',')
                        .append(scenarioIds[scenarioIndex]).append(',')
                        .append(categories[scenarioIndex]).append(',')
                        .append(matrix.score(candidateIndex, scenarioIndex))
                        .append('\n');
            }
        }
        return out.toString();
    }
}
