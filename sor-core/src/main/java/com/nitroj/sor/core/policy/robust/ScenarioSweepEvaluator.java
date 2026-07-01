package com.nitroj.sor.core.policy.robust;

/**
 * Responsibility: evaluate candidate policies against a named scenario set.
 */
@FunctionalInterface
public interface ScenarioSweepEvaluator {
    ScoreMatrix evaluate(PolicyCandidateSet candidates, ScenarioSetDescriptor scenarios);
}
