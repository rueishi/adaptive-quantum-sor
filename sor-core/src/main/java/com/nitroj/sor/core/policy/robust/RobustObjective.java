package com.nitroj.sor.core.policy.robust;

/**
 * Responsibility: pure robust-selection objective over a score matrix.
 */
public interface RobustObjective {
    RobustSelection select(ScoreMatrix matrix);

    static RobustObjective fromConfig(final RobustSelectionConfig config) {
        final RobustSelectionConfig resolved = config == null ? RobustSelectionConfig.defaults() : config;
        return switch (resolved.objective()) {
            case EXPECTED, ADEQUACY_FALLBACK -> new ExpectedObjective(resolved.objective(), "mean");
            case MIN_MAX -> new MinMaxObjective();
            case CVAR_K -> new CvarKObjective(resolved.cvarKPercent());
            case MIN_REGRET -> new MinRegretObjective();
            case SINGLE_CANDIDATE_FALLBACK -> new ExpectedObjective(RobustObjectiveType.SINGLE_CANDIDATE_FALLBACK, "single");
        };
    }
}
