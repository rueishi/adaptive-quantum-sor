package com.nitroj.sor.testkit.sim;

import com.nitroj.sor.testkit.sim.scenario.SimChildOrder;
import com.nitroj.sor.testkit.sim.scenario.SimConfig;
import com.nitroj.sor.testkit.sim.scenario.venues.SimVenueOutcome;
import com.nitroj.sor.testkit.sim.scenario.SimulatedOrderInjector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies simulator integration and scenario responsibilities stay separated by package. */
final class SimulatorPackageBoundaryTest {
    @Test
    void scenarioTypesLiveInScenarioPackage() {
        assertEquals("com.nitroj.sor.testkit.sim.scenario", SimConfig.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.scenario", SimChildOrder.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.scenario.venues", SimVenueOutcome.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.scenario", SimulatedOrderInjector.class.getPackageName());
    }

    @Test
    void simulatorProductionSourcesDoNotLiveAtPackageRoot() throws IOException {
        try (var paths = Files.list(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim"))) {
            final var rootJavaSources = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            assertEquals(java.util.List.of(), rootJavaSources);
        }
    }
}
