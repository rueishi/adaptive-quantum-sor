package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.scenario.ScenarioRunner;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSummary;

import java.util.List;

/**
 * Responsibility: replay test-server scenarios into a robust score matrix.
 */
public final class ScenarioRunnerSweepEvaluator implements ScenarioSweepEvaluator {
    private final ScenarioRunner runner;
    private final ScenarioScorecardV1 scorecard;
    private final List<ScenarioSpec> scenarioSpecs;

    public ScenarioRunnerSweepEvaluator(final List<ScenarioSpec> scenarioSpecs) {
        this(new ScenarioRunner(), new ScenarioScorecardV1(), scenarioSpecs);
    }

    public ScenarioRunnerSweepEvaluator(
            final ScenarioRunner runner,
            final ScenarioScorecardV1 scorecard,
            final List<ScenarioSpec> scenarioSpecs
    ) {
        if (runner == null || scorecard == null || scenarioSpecs == null || scenarioSpecs.isEmpty()) {
            throw new IllegalArgumentException("runner, scorecard, and scenarioSpecs must be supplied");
        }
        this.runner = runner;
        this.scorecard = scorecard;
        this.scenarioSpecs = List.copyOf(scenarioSpecs);
    }

    @Override
    public ScoreMatrix evaluate(final PolicyCandidateSet candidates, final ScenarioSetDescriptor scenarios) {
        if (candidates == null || scenarios == null) {
            throw new IllegalArgumentException("candidates and scenarios must not be null");
        }
        if (scenarios.size() != scenarioSpecs.size()) {
            throw new IllegalArgumentException("scenario descriptor size must match supplied specs");
        }
        final long[][] scores = new long[candidates.size()][scenarios.size()];
        for (int scenarioIndex = 0; scenarioIndex < scenarios.size(); scenarioIndex++) {
            final ScenarioSpec spec = scenarioSpecs.get(scenarioIndex);
            if (!spec.scenarioId().equals(scenarios.scenarioId(scenarioIndex))) {
                throw new IllegalArgumentException("scenario descriptor id does not match spec at " + scenarioIndex);
            }
            final ScenarioSummary summary = runner.run(spec);
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
