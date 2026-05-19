package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.scenario.ScenarioRunner;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSummary;

/**
 * Responsibility: replay the declared scenario set into a Phase 7 score matrix.
 */
public final class ScenarioSweepEvaluator {
    private final ScenarioRunner runner;
    private final ScenarioScorecardV1 scorecard;

    public ScenarioSweepEvaluator() {
        this(new ScenarioRunner(), new ScenarioScorecardV1());
    }

    public ScenarioSweepEvaluator(final ScenarioRunner runner, final ScenarioScorecardV1 scorecard) {
        if (runner == null || scorecard == null) {
            throw new IllegalArgumentException("runner and scorecard must not be null");
        }
        this.runner = runner;
        this.scorecard = scorecard;
    }

    public ScoreMatrix evaluate(final PolicyCandidateSet candidates, final ScenarioSetDescriptor scenarios) {
        if (candidates == null || scenarios == null) {
            throw new IllegalArgumentException("candidates and scenarios must not be null");
        }
        final long[][] scores = new long[candidates.size()][scenarios.size()];
        for (int scenarioIndex = 0; scenarioIndex < scenarios.size(); scenarioIndex++) {
            final ScenarioSummary summary = runner.run(scenarios.scenarios().get(scenarioIndex));
            for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                scores[candidateIndex][scenarioIndex] = scorecard.score(
                        summary,
                        candidates.candidates().get(candidateIndex).compiledPolicy()
                );
            }
        }
        return ScoreMatrix.of(
                scenarios.scenarioSetId(),
                scenarios.scenarioSetVersion(),
                ScenarioScorecardV1.VERSION,
                candidates.candidates(),
                scenarios,
                scores
        );
    }
}
