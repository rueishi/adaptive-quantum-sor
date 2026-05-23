package com.nitroj.sor.sim;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SimulatorsZeroDependencyTest {
    @Test
    void buildDeclaresSimulatorApiAndSampleServerDependencies() throws IOException {
        final String build = Files.readString(Path.of("sor-test-server/build.gradle"));

        assertEquals(1, count(build, "project(':sor-api')"));
        assertEquals(1, count(build, "implementation project(':sor-core')"));
        assertEquals(1, count(build, "project(':sor-observability')"));
        assertEquals(1, count(build, "project(':sor-transport-aeron')"));
        assertEquals(1, count(build, "project(':sor-transport-http-control')"));
    }

    @Test
    void nonServerMainSourcesDoNotReferenceLegacyCorePackages() throws IOException {
        try (var paths = Files.walk(Path.of("sor-test-server/src/main/java/com/nitroj/sor/sim"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/com/nitroj/sor/sim/server/"))
                    .filter(path -> read(path).contains("com.nitroj.adaptive.quantum.sor")
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
