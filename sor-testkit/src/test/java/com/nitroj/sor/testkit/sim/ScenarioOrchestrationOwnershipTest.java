package com.nitroj.sor.testkit.sim;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifies scenario orchestration/result classes stay out of the simulator package. */
final class ScenarioOrchestrationOwnershipTest {
    @Test
    void simulatorModuleDoesNotOwnScenarioRunnerOrRunResult() {
        assertFalse(Files.exists(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim/ScenarioRunner.java")));
        assertFalse(Files.exists(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim/ScenarioRunResult.java")));
    }
}
