package com.nitroj.sor.testkit.sim;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies simulators zero dependency behavior for the reusable SOR testkit module.
 *
 * <p>Run with :sor-testkit:test to keep module boundaries and deterministic fixtures stable.</p>
 */
class SimulatorsZeroDependencyTest {
    @Test
    void buildDeclaresSimulatorApiAndCoreDependenciesOnly() throws IOException {
        final String build = Files.readString(Path.of("sor-testkit/build.gradle"));

        assertEquals(1, count(build, "project(':sor-api')"));
        assertEquals(1, count(build, "project(':sor-core')"));
        assertEquals(0, count(build, "project(':sor-observability')"));
        assertEquals(0, count(build, "project(':sor-transport-aeron')"));
        assertEquals(0, count(build, "project(':sor-transport-http-control')"));
        assertEquals(0, count(build, "project(':sor-test-server')"));
    }

    @Test
    void nonServerMainSourcesDoNotReferenceLegacyCorePackages() throws IOException {
        try (var paths = Files.walk(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains("com.nitroj.sor.core")
                            || read(path).contains("com.nitroj.sor.core"))
                    .toList();

            assertEquals(java.util.List.of(), offenders);
        }
    }

    private static int count(final String text, final String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
