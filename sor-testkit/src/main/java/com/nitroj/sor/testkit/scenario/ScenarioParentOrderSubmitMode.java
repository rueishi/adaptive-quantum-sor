package com.nitroj.sor.testkit.scenario;

/** Submit path for parent orders supplied with a live scenario run. */
public enum ScenarioParentOrderSubmitMode {
    SIMULATED,
    API;

    public static ScenarioParentOrderSubmitMode parse(final String value) {
        if ("SIMULATED".equalsIgnoreCase(value)) {
            return SIMULATED;
        }
        if ("API".equalsIgnoreCase(value)) {
            return API;
        }
        throw new IllegalArgumentException("submitMode must be SIMULATED or API");
    }
}
