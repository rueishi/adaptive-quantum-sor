package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Responsibility: describe one replayable scenario script.
 *
 * <p>Role in system: this is the immutable contract passed to
 * {@link ScenarioRunner}. It captures scenario identity, seed, duration,
 * dimensions, regime windows, venue-profile behavior, and simulator defaults.</p>
 *
 * <p>Relationships: owns {@link ScenarioWindow} schedule and
 * {@link ScenarioSimulatorConfig}; creates deterministic profile assignments
 * through {@link ScenarioVenueProfile}.</p>
 *
 * <p>Lifecycle: constructed before a scenario run and never mutated.</p>
 *
 * <p>Design intent: explicit immutable scenario scripts make Gradle/JUnit
 * replay deterministic and keep orchestration separate from low-level
 * simulators.</p>
 */
public final class ScenarioSpec {
    private final String scenarioId;
    private final long seed;
    private final int ticks;
    private final SorConfig config;
    private final ScenarioWindow[] windows;
    private final boolean venueProfilesEnabled;
    private final ScenarioSimulatorConfig simulatorConfig;

    public ScenarioSpec(
            final String scenarioId,
            final long seed,
            final int ticks,
            final SorConfig config,
            final ScenarioWindow[] windows,
            final boolean venueProfilesEnabled,
            final ScenarioSimulatorConfig simulatorConfig
    ) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be positive");
        }
        if (config == null || windows == null || simulatorConfig == null) {
            throw new IllegalArgumentException("config, windows, and simulatorConfig must not be null");
        }
        this.scenarioId = scenarioId;
        this.seed = seed;
        this.ticks = ticks;
        this.config = config;
        this.windows = windows.clone();
        this.venueProfilesEnabled = venueProfilesEnabled;
        this.simulatorConfig = simulatorConfig;
        validateWindows();
    }

    public static ScenarioSpec defaultSpec(final SorConfig config, final long seed) {
        return new ScenarioSpec(
                "default-normal",
                seed,
                1,
                config,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 0)},
                true,
                ScenarioSimulatorConfig.defaults()
        );
    }

    public String scenarioId() {
        return scenarioId;
    }

    public long seed() {
        return seed;
    }

    public int ticks() {
        return ticks;
    }

    public SorConfig config() {
        return config;
    }

    public ScenarioWindow[] windows() {
        return windows.clone();
    }

    public boolean venueProfilesEnabled() {
        return venueProfilesEnabled;
    }

    public ScenarioSimulatorConfig simulatorConfig() {
        return simulatorConfig;
    }

    public int regimeAt(final int tick) {
        if (tick < 0 || tick >= ticks) {
            throw new IndexOutOfBoundsException("tick out of range: " + tick);
        }
        for (ScenarioWindow window : windows) {
            if (window.contains(tick)) {
                return window.regimeId();
            }
        }
        return 0;
    }

    public ScenarioVenueProfile profileForVenue(final int venueId) {
        if (venueId < 0 || venueId >= config.venueCount()) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
        return venueProfilesEnabled ? ScenarioVenueProfile.forVenue(venueId) : ScenarioVenueProfile.TIGHT_DEEP;
    }

    private void validateWindows() {
        final ScenarioWindow[] sorted = windows.clone();
        Arrays.sort(sorted, Comparator.comparingInt(ScenarioWindow::startTickInclusive));
        int previousEnd = -1;
        for (ScenarioWindow window : sorted) {
            if (window.endTickInclusive() >= ticks) {
                throw new IllegalArgumentException("scenario window exceeds tick range");
            }
            if (window.regimeId() >= config.regimeCount()) {
                throw new IllegalArgumentException("regimeId must be within configured regimeCount");
            }
            if (window.startTickInclusive() <= previousEnd) {
                throw new IllegalArgumentException("scenario windows must not overlap");
            }
            previousEnd = window.endTickInclusive();
        }
    }
}
