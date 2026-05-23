package com.nitroj.sor.optnative;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NoJniHeadersTest {
    @Test
    void noGeneratedJniHeadersRemain() throws Exception {
        try (var paths = Files.walk(Path.of("cpp"))) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".h"))
                    .filter(path -> read(path).contains("JNIEXPORT") || read(path).contains("Java_"))
                    .toList();
            assertFalse(!offenders.isEmpty(), "JNI header files remain: " + offenders);
        }
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
