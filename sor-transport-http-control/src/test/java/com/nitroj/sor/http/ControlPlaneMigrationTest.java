package com.nitroj.sor.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies migration behavior from notebook/test endpoints to reusable HTTP control-plane endpoints.
 *
 * <p>Run with transport HTTP tests to keep Phase 9 endpoint ownership clear.</p>
 */
class ControlPlaneMigrationTest {
    @Test
    void legacyNotebookRuntimeArtifactsAreGone() {
        assertFalse(Files.exists(Path.of("sor-core/src/main/java/com/nitroj/adaptive/quantum/sor/SorEngineRuntime.java")));
        assertFalse(Files.exists(Path.of("sor-core/src/main/java/com/nitroj/adaptive/quantum/sor/api/NotebookDemoApiLauncher.java")));
    }

    @Test
    void transportControlPlaneOwnsNewHttpServer() {
        assertTrue(Files.exists(Path.of(
                "sor-transport-http-control/src/main/java/com/nitroj/sor/http/HttpControlPlaneServer.java")));
    }
}
