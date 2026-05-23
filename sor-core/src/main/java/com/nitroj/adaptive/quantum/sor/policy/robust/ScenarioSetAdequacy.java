package com.nitroj.adaptive.quantum.sor.policy.robust;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsibility: evaluate whether a scenario set can support a robustness claim.
 */
public final class ScenarioSetAdequacy {
    public Result evaluate(final ScenarioSetDescriptor scenarios, final RobustSelectionConfig.Adequacy config) {
        if (scenarios == null || config == null) {
            throw new IllegalArgumentException("scenarios and config must not be null");
        }
        final Set<String> present = new LinkedHashSet<>(scenarios.categories());
        final Set<String> missing = new LinkedHashSet<>();
        for (String required : config.requiredCategories()) {
            if (!present.contains(required)) {
                missing.add(required);
            }
        }
        final int countShortfall = Math.max(0, config.minScenarioCount() - scenarios.size());
        final boolean adequate = countShortfall == 0 && missing.isEmpty();
        return new Result(
                adequate ? "ADEQUATE" : "INADEQUATE",
                adequate,
                countShortfall,
                List.copyOf(missing)
        );
    }

    public record Result(
            String status,
            boolean adequate,
            int scenarioCountShortfall,
            List<String> missingCategories
    ) {
    }
}
