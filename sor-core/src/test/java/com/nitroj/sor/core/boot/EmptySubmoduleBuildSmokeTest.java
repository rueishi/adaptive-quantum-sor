package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies that every P8-02 placeholder module has the minimal
 * file structure needed for Gradle to build an empty jar.
 *
 * <p>Role in system: this is the lightweight test counterpart to
 * `./gradlew build`; it avoids recursively launching Gradle from inside Gradle
 * while still proving the placeholder modules exist.</p>
 *
 * <p>Relationships: complements {@link MultiProjectLayoutTest}; settings prove
 * the project is included, while this test proves the included project has a
 * real directory and build file.</p>
 *
 * <p>Lifecycle: runs after P8-02 creates empty module directories.</p>
 *
 * <p>Design intent: make missing placeholder modules fail with a precise path
 * before full build validation reaches them.</p>
 */
class EmptySubmoduleBuildSmokeTest {
    private static final List<String> EMPTY_MODULES = List.of(
            "sor-api",
            "sor-codec",
            "sor-transport-aeron",
            "sor-transport-http-control",
            "sor-optimizers-native",
            "sor-observability",
            "sor-test-server",
            "sor-client-java"
    );

    /**
     * Verifies every placeholder module directory and build script exists.
     */
    @Test
    void emptySubmodulesHaveBuildFiles() {
        for (String module : EMPTY_MODULES) {
            final Path moduleDir = Path.of(module);
            assertTrue(Files.isDirectory(moduleDir), () -> "missing module directory " + moduleDir);
            assertTrue(Files.isRegularFile(moduleDir.resolve("build.gradle")),
                    () -> "missing placeholder build.gradle for " + module);
        }
    }
}
