package com.nitroj.sor.core.policy.robust;

import java.nio.file.Path;
import java.util.List;

/**
 * Responsibility: hold Phase 7 robust-selection configuration resolved outside L0.
 */
public record RobustSelectionConfig(
        boolean enabled,
        RobustObjectiveType objective,
        int cvarKPercent,
        boolean allowMinMaxObjective,
        String scenarioSetId,
        long scenarioSetVersion,
        List<String> scenarioTags,
        CandidateGrid candidateGrid,
        Adequacy adequacy,
        Path artifactDirectory
) {
    public RobustSelectionConfig {
        objective = objective == null ? RobustObjectiveType.CVAR_K : objective;
        scenarioTags = scenarioTags == null ? List.of() : List.copyOf(scenarioTags);
        candidateGrid = candidateGrid == null ? CandidateGrid.defaults() : candidateGrid;
        adequacy = adequacy == null ? Adequacy.defaults() : adequacy;
        artifactDirectory = artifactDirectory == null
                ? Path.of("build/robust-selection/score-matrix")
                : artifactDirectory;
        if (cvarKPercent < 1 || cvarKPercent > 100) {
            throw new IllegalArgumentException("cvarKPercent must be in 1..100");
        }
        if (objective == RobustObjectiveType.MIN_MAX && !allowMinMaxObjective) {
            throw new IllegalArgumentException("MIN_MAX requires allowMinMaxObjective=true");
        }
        if (scenarioSetId == null || scenarioSetId.isBlank()) {
            throw new IllegalArgumentException("scenarioSetId must not be blank");
        }
        if (scenarioSetVersion <= 0) {
            throw new IllegalArgumentException("scenarioSetVersion must be positive");
        }
    }

    public static RobustSelectionConfig defaults() {
        return new RobustSelectionConfig(
                false,
                RobustObjectiveType.CVAR_K,
                10,
                false,
                "default-robust-v1",
                1L,
                List.of("robust"),
                CandidateGrid.defaults(),
                Adequacy.defaults(),
                Path.of("build/robust-selection/score-matrix")
        );
    }

    public record CandidateGrid(
            int[] riskScaleBps,
            int[] concentrationPenaltyScaleBps,
            int maxCandidateCount
    ) {
        public CandidateGrid {
            riskScaleBps = copyNonEmpty("riskScaleBps", riskScaleBps);
            concentrationPenaltyScaleBps = copyNonEmpty("concentrationPenaltyScaleBps", concentrationPenaltyScaleBps);
            if (maxCandidateCount <= 0) {
                throw new IllegalArgumentException("maxCandidateCount must be positive");
            }
        }

        public static CandidateGrid defaults() {
            return new CandidateGrid(
                    new int[]{7_500, 10_000, 12_500},
                    new int[]{7_500, 10_000, 12_500},
                    9
            );
        }

        private static int[] copyNonEmpty(final String name, final int[] values) {
            if (values == null || values.length == 0) {
                throw new IllegalArgumentException(name + " must not be empty");
            }
            final int[] copy = values.clone();
            for (int value : copy) {
                if (value <= 0) {
                    throw new IllegalArgumentException(name + " values must be positive");
                }
            }
            return copy;
        }
    }

    public record Adequacy(
            int minScenarioCount,
            List<String> requiredCategories,
            boolean strictBlock
    ) {
        public Adequacy {
            if (minScenarioCount < 0) {
                throw new IllegalArgumentException("minScenarioCount must be non-negative");
            }
            if (requiredCategories == null) {
                requiredCategories = List.of();
            }
            for (String category : requiredCategories) {
                if (category == null || category.isBlank()) {
                    throw new IllegalArgumentException("required category must not be blank");
                }
            }
            requiredCategories = List.copyOf(requiredCategories);
        }

        public static Adequacy defaults() {
            return new Adequacy(12, List.of("regime", "liquidity", "venue-health", "failure-negative"), false);
        }
    }
}
