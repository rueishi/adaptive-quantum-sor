package com.nitroj.sor.testserver;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies JibImageBuild behavior for the runnable SOR test server.
 *
 * <p>Run with :sor-test-server:test to protect notebook, container, and integration-server workflows.</p>
 */
class JibImageBuildTest {
    @Test
    void buildConfigContainsExpectedImageAndEntrypoint() throws Exception {
        final String build = Files.readString(Path.of("sor-test-server/build.gradle"));
        assertTrue(build.contains("adaptive-quantum-sor/sor-test-server"));
        assertTrue(build.contains("eclipse-temurin:25-jre"));
        assertTrue(build.contains("'-XX:+UseZGC', '-XX:+UseCompactObjectHeaders', '-XX:+AlwaysPreTouch'"));
        assertTrue(build.contains("com.nitroj.sor.testserver.SimulatorServerApplication"));
    }
}
