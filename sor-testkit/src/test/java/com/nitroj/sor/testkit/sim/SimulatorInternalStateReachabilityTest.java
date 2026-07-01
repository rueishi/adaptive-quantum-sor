package com.nitroj.sor.testkit.sim;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies simulator internal state reachability behavior for the reusable SOR testkit module.
 *
 * <p>Run with :sor-testkit:test to keep module boundaries and deterministic fixtures stable.</p>
 */
class SimulatorInternalStateReachabilityTest {
    @Test
    void simulatorSourcesDoNotReachLegacyInternalStateTypes() throws IOException {
        final Set<String> forbidden = Set.of("MarketBookState", "VenueSessionState", "RiskLimitSnapshot");
        try (var paths = Files.walk(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> forbidden.stream().anyMatch(token -> read(path).contains(token)))
                    .toList();

            assertEquals(java.util.List.of(), offenders);
        }
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
