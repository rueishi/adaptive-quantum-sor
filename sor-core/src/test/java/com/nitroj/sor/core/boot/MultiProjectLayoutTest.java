package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies that the P8-02 Gradle settings file exposes the
 * complete Phase 8 project graph.
 *
 * <p>Role in system: this test protects the mechanical module split before
 * later cards add implementation to the individual modules.</p>
 *
 * <p>Relationships: complements {@link EmptySubmoduleBuildSmokeTest}, which
 * checks that every included placeholder module has a build file.</p>
 *
 * <p>Lifecycle: runs from `sor-core` after the legacy test suite moves into
 * that module.</p>
 *
 * <p>Design intent: keep module topology drift visible through a focused unit
 * failure rather than a later missing-project Gradle error.</p>
 */
class MultiProjectLayoutTest {
    private static final List<String> EXPECTED_PROJECTS = List.of(
            "sor-api",
            "sor-core",
            "sor-codec",
            "sor-transport-aeron",
            "sor-transport-http-control",
            "sor-optimizers-native",
            "sor-observability",
            "sor-test-server",
            "sor-client-java"
    );

    /**
     * Parses `settings.gradle` text and verifies every Phase 8 module is
     * included.
     *
     * @throws IOException if settings cannot be read
     */
    @Test
    void settingsIncludesEveryPhaseEightModule() throws IOException {
        final String settings = Files.readString(Path.of("settings.gradle"));

        for (String project : EXPECTED_PROJECTS) {
            assertTrue(settings.contains("include '" + project + "'"),
                    () -> "settings.gradle missing include for " + project);
        }
    }
}
