package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.core.config.SorConfig;

/**
 * Responsibility: represent one human-readable scenario metadata file.
 *
 * <p>Role in system: scenario YAML files are user-facing artifacts, while
 * {@link ScenarioSpec} remains the validated runtime representation consumed by
 * {@link ScenarioRunner}.</p>
 *
 * <p>Relationships: produced by {@link ScenarioDefinitionLoader}; converted to
 * {@link ScenarioSpec} after validation.</p>
 *
 * <p>Lifecycle: created at scenario load time and discarded after the runtime
 * spec is built.</p>
 *
 * <p>Design intent: keep the editable scenario surface friendly and explicit:
 * scenario id, description, seed, ticks, profile behavior, and named regime
 * windows.</p>
 */
public final class ScenarioDefinition {
    private final String scenarioId;
    private final String description;
    private final long seed;
    private final int ticks;
    private final boolean venueProfilesEnabled;
    private final ScenarioWindow[] windows;
    private final ScenarioParentOrderIntent[] parentOrders;

    public ScenarioDefinition(
            final String scenarioId,
            final String description,
            final long seed,
            final int ticks,
            final boolean venueProfilesEnabled,
            final ScenarioWindow[] windows,
            final ScenarioParentOrderIntent[] parentOrders
    ) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be positive");
        }
        if (windows == null || windows.length == 0) {
            throw new IllegalArgumentException("windows must not be empty");
        }
        this.scenarioId = scenarioId;
        this.description = description;
        this.seed = seed;
        this.ticks = ticks;
        this.venueProfilesEnabled = venueProfilesEnabled;
        this.windows = windows.clone();
        this.parentOrders = parentOrders == null ? new ScenarioParentOrderIntent[0] : parentOrders.clone();
    }

    public ScenarioDefinition(
            final String scenarioId,
            final String description,
            final long seed,
            final int ticks,
            final boolean venueProfilesEnabled,
            final ScenarioWindow[] windows
    ) {
        this(scenarioId, description, seed, ticks, venueProfilesEnabled, windows, new ScenarioParentOrderIntent[0]);
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String description() {
        return description;
    }

    public long seed() {
        return seed;
    }

    public int ticks() {
        return ticks;
    }

    public boolean venueProfilesEnabled() {
        return venueProfilesEnabled;
    }

    public ScenarioWindow[] windows() {
        return windows.clone();
    }

    public ScenarioParentOrderIntent[] parentOrders() {
        return parentOrders.clone();
    }

    /**
     * Converts this user-facing definition into the runtime scenario spec.
     */
    public ScenarioSpec toSpec(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        return new ScenarioSpec(
                scenarioId,
                seed,
                ticks,
                config,
                windows,
                venueProfilesEnabled,
                ScenarioSimulatorConfig.defaults()
        );
    }
}
