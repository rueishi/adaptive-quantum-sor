package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the Phase 9 production JVM profile enables Compact
 * Object Headers together with ZGC.
 *
 * <p>Role in system: P9-17 refreshes the runtime baseline from plain JDK
 * 25/ZGC to JDK 25/ZGC/Compact Object Headers. This test proves the configured
 * JVM accepts the flag combination and reports both flags as enabled.</p>
 *
 * <p>Relationships: complements `docs/testing/PHASE_8_JMH_BASELINE.md`, the
 * production launcher, Gradle test JVM arguments, and container metadata.</p>
 *
 * <p>Lifecycle: runs as an integration-style unit test; it starts and waits for
 * two short-lived Java processes.</p>
 *
 * <p>Design intent: make a stale or incompatible JDK fail at the boot-test
 * layer before benchmark evidence is trusted.</p>
 */
class ZgcCompactHeadersCompatibilityTest {
    /**
     * Launches a child JVM with the production GC/header profile and checks
     * startup succeeds on a production JDK 25 runtime.
     *
     * @throws IOException if the child JVM cannot be started
     * @throws InterruptedException if the current test thread is interrupted
     */
    @Test
    void productionJdkAcceptsZgcAndCompactObjectHeaders() throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(
                javaExecutable(),
                "-XX:+UseZGC",
                "-XX:+UseCompactObjectHeaders",
                "-version")
                .redirectErrorStream(true)
                .start();

        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "child JVM did not exit within 10 seconds");
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(0, process.exitValue(), () -> "expected production flags to start, output was:\n" + output);
        assertTrue(output.contains("openjdk version \"25."),
                () -> "expected Java 25 version output, output was:\n" + output);
    }

    /**
     * Reads PrintFlagsFinal from a child JVM and verifies the production
     * runtime profile reports both ZGC and Compact Object Headers as enabled.
     *
     * @throws IOException if the child JVM cannot be started
     * @throws InterruptedException if the current test thread is interrupted
     */
    @Test
    void printFlagsFinalReportsProductionRuntimeProfile() throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(
                javaExecutable(),
                "-XX:+UseZGC",
                "-XX:+UseCompactObjectHeaders",
                "-XX:+PrintFlagsFinal",
                "-version")
                .redirectErrorStream(true)
                .start();

        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "child JVM did not exit within 10 seconds");
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(0, process.exitValue(), () -> "expected PrintFlagsFinal to succeed, output was:\n" + output);
        assertTrue(output.contains("UseZGC") && output.contains("= true"),
                () -> "expected UseZGC=true in PrintFlagsFinal output:\n" + output);
        assertTrue(output.contains("UseCompactObjectHeaders") && output.contains("= true"),
                () -> "expected UseCompactObjectHeaders=true in PrintFlagsFinal output:\n" + output);
    }

    /**
     * Verifies project launch and benchmark documentation carry the production
     * compact-header profile instead of the old Phase 8 disabled state.
     *
     * @throws IOException if a project file cannot be read
     */
    @Test
    void launchSurfacesAndBaselineDocumentCompactHeaderProfile() throws IOException {
        final String launcher = Files.readString(Path.of("scripts/run_engine.sh"));
        final String sampleServer = Files.readString(Path.of("sor-test-server/run_sample_server.sh"));
        final String rootBuild = Files.readString(Path.of("build.gradle"));
        final String baseline = Files.readString(Path.of("docs/testing/PHASE_8_JMH_BASELINE.md"));

        assertTrue(launcher.contains("-XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch"),
                "production launcher must enable ZGC, compact headers, and pre-touch together");
        assertTrue(sampleServer.contains("-XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch"),
                "sample server launcher must enable ZGC, compact headers, and pre-touch together");
        assertTrue(rootBuild.contains("'-XX:+UseCompactObjectHeaders'"),
                "Gradle test JVMs must enable compact headers");
        assertTrue(baseline.contains("Compact Object Headers are enabled for the Phase 9 runtime profile"),
                "baseline documentation must record the compact-header profile");
    }

    /**
     * Resolves the same Java executable used by the running test VM.
     */
    private static String javaExecutable() {
        return System.getProperty("java.home") + "/bin/java";
    }
}
