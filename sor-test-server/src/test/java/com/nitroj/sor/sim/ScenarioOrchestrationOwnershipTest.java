package com.nitroj.sor.sim;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifies scenario orchestration/result classes stay out of the simulator package. */
final class ScenarioOrchestrationOwnershipTest {
    @Test
    void simulatorModuleDoesNotOwnScenarioRunnerOrRunResult() {
        assertFalse(Files.exists(Path.of("sor-test-server/src/main/java/com/nitroj/sor/sim/ScenarioRunner.java")));
        assertFalse(Files.exists(Path.of("sor-test-server/src/main/java/com/nitroj/sor/sim/ScenarioRunResult.java")));
    }
}
