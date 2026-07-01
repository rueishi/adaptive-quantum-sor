package com.nitroj.sor.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies naming conventions for the HTTP control module.
 *
 * <p>Run with architecture tests to keep module identity and package naming stable.</p>
 */
class ModuleNamingTest {
    @Test
    void readmeDocumentsControlPlaneOnly() throws Exception {
        final String readme = Files.readString(Path.of("sor-transport-http-control/README.md"));

        assertTrue(readme.contains("built-in HTTP control plane for users"));
        assertTrue(readme.contains("research and ops only"));
        assertTrue(readme.contains("not the production ultra-low-latency order"));
        assertTrue(readme.contains("Scenario/demo endpoints stay in `sor-test-server`"));
    }
}
