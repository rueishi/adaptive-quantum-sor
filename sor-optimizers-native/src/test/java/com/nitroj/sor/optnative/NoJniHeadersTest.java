package com.nitroj.sor.optnative;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the module no longer depends on JNI header generation.
 *
 * <p>Run with native optimizer tests to protect the Panama/FFM migration boundary.</p>
 */
class NoJniHeadersTest {
    @Test
    void noJniBridgeArtifactsRemain() throws Exception {
        try (var paths = Files.walk(Path.of("cpp"))) {
            final var offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        final String source = read(path);
                        return source.contains("JNIEXPORT")
                                || source.contains("Java_")
                                || source.contains("find_package(JNI")
                                || source.contains("JNI::JNI")
                                || path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).contains("jni");
                    })
                    .toList();
            assertFalse(!offenders.isEmpty(), "JNI bridge artifacts remain: " + offenders);
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
