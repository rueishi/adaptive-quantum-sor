package com.nitroj.adaptive.quantum.sor.scenario;

/**
 * Responsibility: expose live scenario run status to notebooks and API tests.
 *
 * <p>Role in system: combines reset evidence, replay-safety status, success
 * flag, and deterministic summary.</p>
 *
 * <p>Relationships: produced by {@link ScenarioControlService} and serialized
 * by API handlers.</p>
 *
 * <p>Lifecycle: immutable per live scenario run.</p>
 *
 * <p>Design intent: failed or append-mode runs are visible and cannot be
 * mistaken for exact replay evidence.</p>
 */
public record ScenarioRunResult(
        boolean success,
        boolean replaySafe,
        String message,
        ScenarioResetSummary resetSummary,
        ScenarioSummary summary,
        ScenarioParentOrderResult[] parentOrderResults
) {
    public ScenarioRunResult {
        if (message == null || resetSummary == null) {
            throw new IllegalArgumentException("message and resetSummary must not be null");
        }
        parentOrderResults = parentOrderResults == null ? new ScenarioParentOrderResult[0] : parentOrderResults.clone();
    }

    public ScenarioRunResult(
            final boolean success,
            final boolean replaySafe,
            final String message,
            final ScenarioResetSummary resetSummary,
            final ScenarioSummary summary
    ) {
        this(success, replaySafe, message, resetSummary, summary, new ScenarioParentOrderResult[0]);
    }

    @Override
    public ScenarioParentOrderResult[] parentOrderResults() {
        return parentOrderResults.clone();
    }

    public String toJson() {
        final String summaryJson = summary == null ? "{}" : "{\"scenarioId\":\"" + summary.scenarioId()
                + "\",\"seed\":" + summary.seed()
                + ",\"ticksRun\":" + summary.ticksRun()
                + ",\"bookChecksum\":" + summary.bookChecksum()
                + ",\"outcomeCount\":" + summary.outcomeCount() + "}";
        final String parentResultsJson = java.util.Arrays.stream(parentOrderResults)
                .map(ScenarioParentOrderResult::toJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"success\":" + success + ",\"replaySafe\":" + replaySafe
                + ",\"message\":\"" + message + "\",\"resetSummary\":" + resetSummary.toJson()
                + ",\"summary\":" + summaryJson
                + ",\"parentOrderResults\":" + parentResultsJson + "}";
    }
}
