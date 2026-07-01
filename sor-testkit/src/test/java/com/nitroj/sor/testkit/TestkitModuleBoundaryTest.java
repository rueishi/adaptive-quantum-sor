package com.nitroj.sor.testkit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the reusable scenario/simulator testkit module
 * boundary.
 *
 * <p>Role in system: protects the Phase 9 decision that scenario replay and
 * simulator fixtures live outside the engine and outside the runnable test
 * server wrapper.</p>
 *
 * <p>Relationships: checks Gradle settings, testkit dependencies, core source
 * imports, and the server wrapper source tree.</p>
 *
 * <p>Lifecycle: executed by `:sor-testkit:test` as a fast structural guard.</p>
 *
 * <p>Design intent: keep scenario concepts reusable for tests while preventing
 * them from becoming part of the production engine API.</p>
 */
final class TestkitModuleBoundaryTest {
    @Test
    void settingsDeclareStandaloneSorTestkitModule() throws IOException {
        final String settings = Files.readString(Path.of("settings.gradle"));
        final String build = Files.readString(Path.of("sor-testkit/build.gradle"));

        assertTrue(settings.contains("include 'sor-testkit'"));
        assertEquals(1, count(build, "project(':sor-api')"));
        assertEquals(1, count(build, "project(':sor-core')"));
        assertEquals(0, count(build, "project(':sor-test-server')"));
    }

    @Test
    void coreAndApiDoNotImportTestkitPackages() throws IOException {
        assertNoSourceContains(Path.of("sor-api/src/main/java"), "com.nitroj.sor.testkit");
        assertNoSourceContains(Path.of("sor-core/src/main/java"), "com.nitroj.sor.testkit");
    }

    @Test
    void reusableScenarioAndSimulatorPackagesLiveInTestkit() throws IOException {
        assertTrue(Files.isDirectory(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/scenario")));
        assertTrue(Files.isDirectory(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim")));
        assertTrue(Files.isDirectory(Path.of("sor-testkit/src/main/resources/scenarios")));
    }

    @Test
    void runnableServerDoesNotOwnReusableScenarioOrSimulatorPackages() {
        assertTrue(!Files.exists(Path.of("sor-test-server/src/main/java/com/nitroj/sor/testserver/scenario")));
        assertTrue(!Files.exists(Path.of("sor-test-server/src/main/java/com/nitroj/sor/sim/adapters")));
        assertTrue(!Files.exists(Path.of("sor-test-server/src/main/java/com/nitroj/sor/sim/scenario")));
        assertTrue(!Files.exists(Path.of("sor-test-server/src/main/resources/scenarios")));
    }

    private static void assertNoSourceContains(final Path root, final String forbidden) throws IOException {
        try (var paths = Files.walk(root)) {
            final var offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> read(path).contains(forbidden))
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

    private static int count(final String text, final String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
