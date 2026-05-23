package com.nitroj.sor.sim;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-30 production simulator sources no longer contain legacy mapping metadata. */
final class NoLegacySimulatorMappingMetadataTest {
    @Test
    void productionSourcesContainNoLegacyMappingMetadata() throws IOException {
        final Set<String> forbidden = Set.of("SimulatorMapping", "legacy =", "com.nitroj.adaptive.quantum.sor.sim");
        try (var paths = Files.walk(Path.of("sor-test-server/src/main/java"))) {
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
