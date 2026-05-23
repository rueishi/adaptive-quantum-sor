package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify scenario specification and simulator default
 * contracts.
 *
 * <p>Role in system: covers P5-TC-001 by proving scenario metadata, windows,
 * venue profiles, and simulator parameters validate before later simulator task
 * cards consume them.</p>
 *
 * <p>Relationships: tests {@link ScenarioSpec}, {@link ScenarioWindow},
 * {@link ScenarioVenueProfile}, and {@link ScenarioSimulatorConfig}.</p>
 *
 * <p>Lifecycle: executed by Gradle as scenario-driven unit coverage.</p>
 *
 * <p>Design intent: keep scenario orchestration tests separate from low-level
 * deterministic simulator tests under the `sim` package.</p>
 */
final class ScenarioSpecTest {
    private static final SorConfig CONFIG = new SorConfig(2, 6, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void scenarioSpecExposesImmutableMetadataProfilesAndRegimeWindows() {
        final ScenarioWindow[] windows = new ScenarioWindow[]{
                new ScenarioWindow(0, 4, 0),
                new ScenarioWindow(5, 7, 1),
                new ScenarioWindow(8, 9, 2)
        };
        final ScenarioSpec spec = new ScenarioSpec(
                "normal-volatile-thin",
                -17L,
                10,
                CONFIG,
                windows,
                true,
                ScenarioSimulatorConfig.defaults()
        );
        windows[0] = new ScenarioWindow(0, 0, 2);
        final ScenarioWindow[] copy = spec.windows();
        copy[0] = new ScenarioWindow(0, 0, 2);

        assertEquals("normal-volatile-thin", spec.scenarioId());
        assertEquals(-17L, spec.seed(), "negative seeds are valid deterministic seeds");
        assertEquals(10, spec.ticks());
        assertSame(CONFIG, spec.config());
        assertEquals(0, spec.regimeAt(0));
        assertEquals(0, spec.regimeAt(4));
        assertEquals(1, spec.regimeAt(5));
        assertEquals(2, spec.regimeAt(9));
        assertEquals(ScenarioVenueProfile.TIGHT_DEEP, spec.profileForVenue(0));
        assertEquals(ScenarioVenueProfile.WIDE_SLOW, spec.profileForVenue(1));
        assertEquals(ScenarioVenueProfile.TIGHT_DEEP, spec.windows()[0].regimeId() == 0 ? spec.profileForVenue(5) : null);
    }

    @Test
    void scenarioMetadataExposesSeedIdTicksProfilesAndRegimeWindows() {
        final ScenarioSpec spec = new ScenarioSpec("metadata", 123L, 3, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 0), new ScenarioWindow(1, 2, 2)},
                false,
                ScenarioSimulatorConfig.defaults());

        assertEquals("metadata", spec.scenarioId());
        assertEquals(123L, spec.seed());
        assertEquals(3, spec.ticks());
        assertFalse(spec.venueProfilesEnabled());
        assertEquals(0, spec.regimeAt(0));
        assertEquals(2, spec.regimeAt(1));
        assertEquals(2, spec.windows().length);
        assertEquals(ScenarioVenueProfile.TIGHT_DEEP, spec.profileForVenue(4));
    }

    @Test
    void invalidScenarioConfigFailsBeforeStateMutation() {
        final ScenarioWindow[] overlapping = new ScenarioWindow[]{
                new ScenarioWindow(0, 1, 0),
                new ScenarioWindow(1, 2, 1)
        };

        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec(
                "bad-overlap", 1L, 3, CONFIG, overlapping, true, ScenarioSimulatorConfig.defaults()));
    }

    @Test
    void scenarioSpecRejectsInvalidIdsTicksWindowsAndRegimes() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec("", 1L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 0)}, true, ScenarioSimulatorConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec("bad", 1L, 0, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 0)}, true, ScenarioSimulatorConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec("bad", 1L, 2, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, 0), new ScenarioWindow(1, 1, 1)}, true,
                ScenarioSimulatorConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec("bad", 1L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, 0)}, true, ScenarioSimulatorConfig.defaults()));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSpec("bad", 1L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, 3)}, true, ScenarioSimulatorConfig.defaults()));
        assertThrows(IndexOutOfBoundsException.class, () -> ScenarioSpec.defaultSpec(CONFIG, 1L).regimeAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> ScenarioSpec.defaultSpec(CONFIG, 1L).profileForVenue(99));
    }

    @Test
    void scenarioWindowAndConfigRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioWindow(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioWindow(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioWindow(0, 0, -1));
        assertTrue(new ScenarioWindow(2, 4, 1).contains(3));
        assertFalse(new ScenarioWindow(2, 4, 1).contains(5));

        assertThrows(IllegalArgumentException.class, () -> new ScenarioSimulatorConfig(-1, 1, 1, 1, 1, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSimulatorConfig(1, 1, 1, 0, 1, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSimulatorConfig(1, 1, 1, 1, -1, 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioSimulatorConfig(1, 1, 1, 1, 1, 1, 10_001, 0, 0));
        assertEquals(1, ScenarioSimulatorConfig.defaults().normalVolatilityTicks());
    }
}
