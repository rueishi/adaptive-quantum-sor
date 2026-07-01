package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifies the old in-core nativebridge package remains retired. */
final class NoLegacyNativeBridgePackageTest {
    @Test
    void legacyNativeBridgePackageIsGone() throws IOException {
        assertFalse(Files.exists(Path.of("sor-core/src/main/java/com/nitroj/adaptive/quantum/sor/nativebridge")));
        assertFalse(Files.exists(Path.of("sor-core/src/test/java/com/nitroj/adaptive/quantum/sor/nativebridge")));

        try (var paths = Files.walk(Path.of("sor-core/src"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.endsWith("NoLegacyNativeBridgePackageTest.java"))
                    .filter(path -> read(path).contains("com.nitroj.sor.core.nativebridge"))
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
