package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify user-readable scenario metadata files.
 *
 * <p>Role in system: proves scenarios are driven by files under
 * {@code scenarios/} instead of requiring users to edit Java constructors.</p>
 *
 * <p>Relationships: exercises {@link ScenarioDefinitionLoader},
 * {@link ScenarioDefinition}, {@link ScenarioSpec}, and {@link ScenarioRunner}.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit with scenario tests.</p>
 *
 * <p>Design intent: every checked-in scenario file must load, validate, and
 * replay deterministically from fixed metadata.</p>
 */
final class ScenarioDefinitionLoaderTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void loadsFriendlyScenarioFileIntoRuntimeSpec() throws Exception {
        final ScenarioDefinition definition = new ScenarioDefinitionLoader()
                .load(Path.of("scenarios/regime/normal_volatile_thin.yaml"));
        final ScenarioSpec spec = definition.toSpec(CONFIG);

        assertEquals("normal-volatile-thin", definition.scenarioId());
        assertTrue(definition.description().contains("Normal market"));
        assertEquals(42L, definition.seed());
        assertEquals(30, definition.ticks());
        assertTrue(definition.venueProfilesEnabled());
        assertEquals(RegimeState.NORMAL, spec.regimeAt(0));
        assertEquals(RegimeState.VOLATILE, spec.regimeAt(10));
        assertEquals(RegimeState.THIN_BOOK, spec.regimeAt(20));
    }

    @Test
    void parsesOptionalScenarioParentOrders() {
        final ScenarioDefinition definition = new ScenarioDefinitionLoader().loadString("""
                scenarioId: parent-order-defaults
                description: Scenario with editable parent order defaults.
                seed: 7
                ticks: 5
                venueProfilesEnabled: true
                parentOrders:
                  - instrumentId: 0
                    side: BUY
                    quantity: 4000
                    urgency: NORMAL
                    atTick: 1
                    submitMode: SIMULATED
                    clientOrderRef: baseline-buy
                  - instrumentId: 1
                    side: SELL
                    quantity: 2500
                    urgencyId: 0
                    submitVia: API
                    clientOrderRef: api-sell
                windows:
                  - name: normal
                    startTick: 0
                    endTick: 4
                    regime: NORMAL
                """);

        assertEquals(2, definition.parentOrders().length);
        assertEquals(Side.BUY, definition.parentOrders()[0].side());
        assertEquals(1, definition.parentOrders()[0].urgencyId());
        assertEquals(1, definition.parentOrders()[0].atTick());
        assertEquals(ScenarioParentOrderSubmitMode.SIMULATED, definition.parentOrders()[0].submitMode());
        assertEquals("baseline-buy", definition.parentOrders()[0].clientOrderRef());
        assertEquals(Side.SELL, definition.parentOrders()[1].side());
        assertEquals(ScenarioParentOrderSubmitMode.API, definition.parentOrders()[1].submitMode());
    }

    @Test
    void allScenarioFilesLoadAndReplayDeterministically() throws Exception {
        final ScenarioDefinitionLoader loader = new ScenarioDefinitionLoader();
        final List<Path> files = scenarioFiles();
        assertFalse(files.isEmpty());
        assertTrue(files.size() >= 62, "scenario catalog should cover at least 62 user-readable scenarios");

        for (Path file : files) {
            final ScenarioSpec spec = loader.load(file).toSpec(CONFIG);
            final ScenarioRunner runner = new ScenarioRunner();
            final ScenarioSummary first = runner.run(spec);
            final ScenarioSummary second = runner.run(spec);

            ScenarioAssertions.assertReplayEquivalent(first, second);
            assertEquals(spec.scenarioId(), first.scenarioId());
            assertEquals(spec.ticks(), first.ticksRun());
        }
    }

    @Test
    void allScenarioFilesDeclareCategoryAndTagsMatchingFolder() throws Exception {
        for (Path file : scenarioFiles()) {
            final String content = Files.readString(file);
            final String category = file.getParent().getFileName().toString();

            assertTrue(content.contains("\ncategory: " + category + "\n"),
                    () -> file + " must declare category matching its folder");
            assertTrue(content.contains("\ntags:\n  - "),
                    () -> file + " must declare user-readable tags");
        }
    }

    @Test
    void phase6BatchAllocationScenariosDocumentHardCases() throws Exception {
        final ScenarioDefinitionLoader loader = new ScenarioDefinitionLoader();

        for (String scenario : new String[]{
                "batch_same_venue_self_impact.yaml",
                "batch_shared_capacity.yaml",
                "batch_correlated_venue_leakage.yaml",
                "batch_infeasible_fallback.yaml"
        }) {
            final Path file = Path.of("scenarios/optimizer-policy", scenario);
            final ScenarioDefinition definition = loader.load(file);
            final String content = Files.readString(file);

            assertTrue(content.contains("\ncategory: optimizer-policy\n"),
                    () -> scenario + " must declare optimizer-policy category");
            assertTrue(content.contains("\n  - phase-6\n"), () -> scenario + " must be tagged phase-6");
            assertTrue(content.contains("\n  - batch-allocation\n"),
                    () -> scenario + " must be tagged batch-allocation");
            assertTrue(content.contains("expected:\n"), () -> scenario + " must document expected evidence");
            assertFalse(definition.parentOrders().length == 0,
                    () -> scenario + " must include user-editable parent order defaults");
        }
    }

    @Test
    void rejectsInvalidScenarioFileBeforeRuntimeStateExists() {
        final ScenarioDefinitionLoader loader = new ScenarioDefinitionLoader();

        assertThrows(IllegalArgumentException.class, () -> loader.loadString("""
                scenarioId:
                description: bad
                seed: 1
                ticks: 0
                venueProfilesEnabled: true
                windows:
                  - name: bad
                    startTick: 0
                    endTick: 0
                    regime: UNKNOWN
                """));
        assertEquals("path must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> loader.load(null)
        ).getMessage());
    }

    private static List<Path> scenarioFiles() throws Exception {
        try (var stream = Files.walk(Path.of("scenarios"))) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yaml"))
                    .sorted()
                    .toList();
        }
    }
}
