package com.nitroj.adaptive.quantum.sor.policy.robust;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: exact-reference tests for Phase 7 robust objectives.
 */
final class RobustObjectiveTest {
    @Test
    void expectedSelectsHighestMeanScore() {
        final RobustSelection selection = new ExpectedObjective().select(matrix(new long[][]{
                {10, 10, 10},
                {0, 30, 30},
                {12, 12, 12}
        }));

        assertEquals(1, selection.selectedCandidateId());
        assertEquals(RobustObjectiveType.EXPECTED, selection.objective());
    }

    @Test
    void minMaxSelectsHighestWorstCaseScore() {
        final RobustSelection selection = new MinMaxObjective().select(matrix(new long[][]{
                {9, 90, 90},
                {20, 20, 20},
                {19, 100, 100}
        }));

        assertEquals(1, selection.selectedCandidateId());
    }

    @Test
    void cvarSelectsHighestWorstTailMeanAndIncludesAtLeastOneScenario() {
        final RobustSelection selection = new CvarKObjective(10).select(matrix(new long[][]{
                {5, 100, 100},
                {20, 25, 30},
                {19, 200, 200}
        }));

        assertEquals(1, selection.selectedCandidateId());
        assertArrayEquals(new double[]{5.0, 20.0, 19.0}, selection.objectiveValues());
    }

    @Test
    void minRegretSelectsSmallestMaximumRegret() {
        final RobustSelection selection = new MinRegretObjective().select(matrix(new long[][]{
                {100, 10},
                {60, 60},
                {10, 100}
        }));

        assertEquals(1, selection.selectedCandidateId());
        assertArrayEquals(new double[]{-90.0, -40.0, -90.0}, selection.objectiveValues());
    }

    @Test
    void tiesChooseLowestCandidateId() {
        final RobustSelection selection = new ExpectedObjective().select(matrix(new int[]{0, 1, 2}, new long[][]{
                {10, 20},
                {15, 15},
                {20, 10}
        }));

        assertEquals(0, selection.selectedCandidateId());
    }

    @Test
    void objectiveResolutionUsesConfigDefaultsAndRejectsInvalidCvar() {
        assertInstanceOf(CvarKObjective.class, RobustObjective.fromConfig(RobustSelectionConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> new CvarKObjective(0));
        assertThrows(IllegalArgumentException.class, () -> new RobustSelectionConfig(
                true,
                RobustObjectiveType.MIN_MAX,
                10,
                false,
                "s",
                1L,
                List.of("robust"),
                RobustSelectionConfig.CandidateGrid.defaults(),
                RobustSelectionConfig.Adequacy.defaults(),
                null
        ));
    }

    private static ScoreMatrix matrix(final long[][] scores) {
        return matrix(new int[]{0, 1, 2}, scores);
    }

    private static ScoreMatrix matrix(final int[] candidateIds, final long[][] scores) {
        final long[] hashes = new long[candidateIds.length];
        final String[] sha = new String[candidateIds.length];
        for (int i = 0; i < candidateIds.length; i++) {
            hashes[i] = 100 + candidateIds[i];
            sha[i] = "ab".repeat(32);
        }
        final String[] scenarioIds = new String[scores[0].length];
        final String[] categories = new String[scores[0].length];
        for (int i = 0; i < scenarioIds.length; i++) {
            scenarioIds[i] = "s" + i;
            categories[i] = i % 2 == 0 ? "regime" : "liquidity";
        }
        return new ScoreMatrix(
                "m",
                "s",
                1L,
                ScenarioScorecardV1.VERSION,
                candidateIds,
                hashes,
                sha,
                scenarioIds,
                categories,
                scores
        );
    }
}
