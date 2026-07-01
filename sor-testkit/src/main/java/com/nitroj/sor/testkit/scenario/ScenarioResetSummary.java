package com.nitroj.sor.testkit.scenario;

import java.util.Arrays;

/**
 * Responsibility: expose visible reset evidence for a live scenario run.
 *
 * <p>Role in system: notebooks and API tests can see exactly what was cleared,
 * kept, and repopulated before a scenario run.</p>
 *
 * <p>Relationships: produced by {@link ScenarioControlService} and embedded in
 * {@link ScenarioRunResult}.</p>
 *
 * <p>Lifecycle: immutable value returned per reset operation.</p>
 *
 * <p>Design intent: make live purge behavior auditable and user-visible.</p>
 */
public record ScenarioResetSummary(
        String scenarioId,
        ScenarioResetMode resetMode,
        boolean replaySafe,
        String[] clearedState,
        String[] keptState,
        String[] repopulatedState
) {
    public ScenarioResetSummary {
        if (scenarioId == null || scenarioId.isBlank() || resetMode == null
                || clearedState == null || keptState == null || repopulatedState == null) {
            throw new IllegalArgumentException("reset summary inputs must not be null or blank");
        }
        clearedState = clearedState.clone();
        keptState = keptState.clone();
        repopulatedState = repopulatedState.clone();
    }

    public String toJson() {
        return "{\"scenarioId\":\"" + scenarioId + "\",\"resetMode\":\"" + resetMode.name()
                + "\",\"replaySafe\":" + replaySafe
                + ",\"clearedState\":\"" + String.join("|", clearedState)
                + "\",\"keptState\":\"" + String.join("|", keptState)
                + "\",\"repopulatedState\":\"" + String.join("|", repopulatedState) + "\"}";
    }

    @Override
    public String toString() {
        return "ScenarioResetSummary{" + scenarioId + ',' + resetMode + ',' + replaySafe + ','
                + Arrays.toString(clearedState) + ',' + Arrays.toString(keptState) + ','
                + Arrays.toString(repopulatedState) + '}';
    }
}
