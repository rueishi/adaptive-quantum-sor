package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies that the Phase 8 module graph only contains owned
 * project dependency edges.
 *
 * <p>Role in system: later Phase 8 cards deliberately add dependencies as
 * framework boundaries become real. This test keeps those edges explicit so an
 * accidental dependency cannot sneak into the graph.</p>
 *
 * <p>Relationships: pairs with Gradle's own project graph resolution; this test
 * gives a small, readable assertion over project dependency declarations.</p>
 *
 * <p>Lifecycle: scans all `build.gradle` files during `sor-core:test`.</p>
 *
 * <p>Design intent: real module dependencies must be added by the task card
 * that owns the implementation requiring them.</p>
 */
class ModuleDependencyGraphTest {
    private static final Pattern PROJECT_DEPENDENCY = Pattern.compile("project\\(['\"]:([^'\"]+)['\"]\\)");
    private static final Set<String> ALLOWED_EDGES = Set.of(
            "sor-core->sor-api",
            "sor-test-server->sor-api",
            "sor-test-server->sor-core",
            "sor-test-server->sor-observability",
            "sor-test-server->sor-transport-aeron",
            "sor-test-server->sor-transport-http-control",
            "sor-observability->sor-api",
            "sor-transport-http-control->sor-api",
            "sor-transport-http-control->sor-observability",
            "sor-transport-aeron->sor-api",
            "sor-transport-aeron->sor-codec",
            "sor-transport-aeron->sor-core",
            "sor-transport-aeron->sor-test-server",
            "sor-client-java->sor-api",
            "sor-client-java->sor-codec",
            "sor-client-java->sor-transport-aeron",
            "sor-client-java->sor-core",
            "sor-client-java->sor-test-server"
    );

    /**
     * Ensures module build scripts only declare project dependencies that have
     * been accepted by the owning Phase 8 task card.
     *
     * @throws IOException if build files cannot be walked
     */
    @Test
    void moduleLayoutContainsOnlyOwnedProjectDependencyEdges() throws IOException {
        try (var paths = Files.walk(Path.of("."))) {
            final var disallowedEdges = paths
                    .filter(path -> path.getFileName().toString().equals("build.gradle"))
                    .flatMap(path -> {
                        final String module = Path.of(".").relativize(path.getParent()).toString()
                                .replace(path.getFileSystem().getSeparator(), ":");
                        final var matcher = PROJECT_DEPENDENCY.matcher(readUnchecked(path));
                        return matcher.results()
                                .map(result -> module + "->" + result.group(1))
                                .filter(edge -> !ALLOWED_EDGES.contains(edge));
                    })
                    .toList();

            assertTrue(disallowedEdges.isEmpty(), "unexpected project dependency edges: " + disallowedEdges);
        }
    }

    /**
     * Keeps the embedded engine package separate from simulator, transport, and
     * simulator-server assembly concerns even while the wider `sor-core` module
     * still carries Phase 1-7 compatibility code.
     *
     * @throws IOException if source files cannot be walked
     */
    @Test
    void embeddedEnginePackageDoesNotReferenceAssemblyOrSimulatorPackages() throws IOException {
        final Set<String> disallowedReferences = Set.of(
                "com.nitroj.sor.sim",
                "com.nitroj.sor.server",
                "com.nitroj.sor.http",
                "com.nitroj.sor.transport",
                "SorServerApplication",
                "SimulatorServerApplication",
                "HttpControlPlaneServer",
                "SorHttpApiServer"
        );

        try (var paths = Files.walk(Path.of("sor-core/src/main/java/com/nitroj/sor/core"))) {
            final var violations = paths
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .flatMap(path -> disallowedReferences.stream()
                            .filter(reference -> readUnchecked(path).contains(reference))
                            .map(reference -> path + " references " + reference))
                    .toList();

            assertTrue(violations.isEmpty(), "embedded engine package crossed a boundary: " + violations);
        }
    }

    /**
     * Reads a build file and converts checked IO failures into assertion
     * failures through an unchecked exception.
     */
    private static String readUnchecked(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("could not read " + path, ex);
        }
    }
}
