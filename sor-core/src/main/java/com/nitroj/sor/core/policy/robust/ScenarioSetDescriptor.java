package com.nitroj.sor.core.policy.robust;

import java.util.List;

/**
 * Responsibility: describe the scenario axis used by a robust score matrix.
 */
public record ScenarioSetDescriptor(
        String scenarioSetId,
        long scenarioSetVersion,
        List<String> scenarioIds,
        List<String> categories
) {
    public ScenarioSetDescriptor {
        if (scenarioSetId == null || scenarioSetId.isBlank()) {
            throw new IllegalArgumentException("scenarioSetId must not be blank");
        }
        if (scenarioSetVersion <= 0) {
            throw new IllegalArgumentException("scenarioSetVersion must be positive");
        }
        if (scenarioIds == null || scenarioIds.isEmpty()) {
            throw new IllegalArgumentException("scenarioIds must not be empty");
        }
        if (categories == null || categories.size() != scenarioIds.size()) {
            throw new IllegalArgumentException("categories must match scenarios");
        }
        for (String scenarioId : scenarioIds) {
            if (scenarioId == null || scenarioId.isBlank()) {
                throw new IllegalArgumentException("scenario id must not be blank");
            }
        }
        for (String category : categories) {
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("scenario category must not be blank");
            }
        }
        scenarioIds = List.copyOf(scenarioIds);
        categories = List.copyOf(categories);
    }

    public int size() {
        return scenarioIds.size();
    }

    public String scenarioId(final int scenarioIndex) {
        return scenarioIds.get(scenarioIndex);
    }

    public String category(final int scenarioIndex) {
        return categories.get(scenarioIndex);
    }
}
