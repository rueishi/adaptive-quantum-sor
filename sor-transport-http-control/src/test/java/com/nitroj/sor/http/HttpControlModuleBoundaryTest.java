package com.nitroj.sor.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: protects the built-in user HTTP module boundary.
 *
 * <p>Role in system: ensures the supported HTTP control module does not depend
 * on simulator or test-server scenario code.</p>
 *
 * <p>Relationships: source-level guard for `sor-transport-http-control`.</p>
 *
 * <p>Lifecycle: runs in `:sor-transport-http-control:test`.</p>
 *
 * <p>Design intent: keep user endpoints separate from notebook/demo scenario
 * endpoints.</p>
 */
class HttpControlModuleBoundaryTest {
    @Test
    void moduleDoesNotDependOnTestServerOrScenarioPackages() throws Exception {
        final String build = Files.readString(Path.of("sor-transport-http-control/build.gradle"));
        final String source = Files.readString(Path.of(
                "sor-transport-http-control/src/main/java/com/nitroj/sor/http/HttpControlPlaneServer.java"));

        assertTrue(!build.contains("sor-test-server"));
        assertTrue(!source.contains("com.nitroj.sor.testserver.scenario"));
        assertTrue(!source.contains("com.nitroj.sor.sim"));
        assertTrue(!source.contains("/scenario/"));
        assertTrue(!source.contains("/events/stream"));
    }
}
