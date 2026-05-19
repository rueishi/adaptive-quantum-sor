package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;

import java.util.List;

/**
 * Responsibility: describe the scenario axis used by a robust score matrix.
 */
public record ScenarioSetDescriptor(
        String scenarioSetId,
        long scenarioSetVersion,
        List<ScenarioSpec> scenarios,
        List<String> categories
) {
    public ScenarioSetDescriptor {
        if (scenarioSetId == null || scenarioSetId.isBlank()) {
            throw new IllegalArgumentException("scenarioSetId must not be blank");
        }
        if (scenarioSetVersion <= 0) {
            throw new IllegalArgumentException("scenarioSetVersion must be positive");
        }
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("scenarios must not be empty");
        }
        if (categories == null || categories.size() != scenarios.size()) {
            throw new IllegalArgumentException("categories must match scenarios");
        }
        for (String category : categories) {
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("scenario category must not be blank");
            }
        }
        scenarios = List.copyOf(scenarios);
        categories = List.copyOf(categories);
    }

    public int size() {
        return scenarios.size();
    }

    public String scenarioId(final int scenarioIndex) {
        return scenarios.get(scenarioIndex).scenarioId();
    }

    public String category(final int scenarioIndex) {
        return categories.get(scenarioIndex);
    }
}
