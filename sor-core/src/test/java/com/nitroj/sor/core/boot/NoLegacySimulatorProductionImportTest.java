package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies P8-28 no production class imports the legacy simulator package. */
final class NoLegacySimulatorProductionImportTest {
    @Test
    void productionSourcesDoNotImportLegacySimulatorPackage() throws IOException {
        try (var paths = Files.walk(Path.of("sor-core/src/main/java"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/sim/"))
                    .filter(path -> read(path).contains("import com.nitroj.sor.core.sim"))
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
