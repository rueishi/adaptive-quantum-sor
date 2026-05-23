package com.nitroj.sor.sim.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JibImageBuildTest {
    @Test
    void buildConfigContainsExpectedImageAndEntrypoint() throws Exception {
        final String build = Files.readString(Path.of("sor-test-server/build.gradle"));
        assertTrue(build.contains("adaptive-quantum-sor/sor-test-server"));
        assertTrue(build.contains("eclipse-temurin:25-jre"));
        assertTrue(build.contains("com.nitroj.sor.sim.server.SimulatorServerApplication"));
    }
}
