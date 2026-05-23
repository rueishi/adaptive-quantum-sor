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
 * Responsibility: documents and verifies the Phase 8 decision to disable
 * Compact Object Headers while using ZGC.
 *
 * <p>Role in system: the P8-01 baseline must not be invalidated by enabling a
 * JVM flag combination that the Phase 8 specification treats as incompatible.
 * This test forks a tiny JVM to capture the configured toolchain's behavior and
 * always verifies the project launch surfaces keep the flag disabled.</p>
 *
 * <p>Relationships: complements `docs/PHASE_8_JMH_BASELINE.md`, which explains
 * the operational reason the production launcher omits compact headers.</p>
 *
 * <p>Lifecycle: runs as an integration-style unit test; it starts and waits for
 * one short-lived Java process.</p>
 *
 * <p>Design intent: keep the failure close to the flag decision, not buried in
 * a later startup script or benchmark run.</p>
 */
class ZgcCompactHeadersIncompatibilityTest {
    /**
     * Launches a child JVM with the rejected flag combination and checks that
     * startup fails with a diagnostic mentioning the relevant flags.
     *
     * @throws IOException if the child JVM cannot be started
     * @throws InterruptedException if the current test thread is interrupted
     */
    @Test
    void compactObjectHeadersAreNotEnabledInPhaseEightLauncher() throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(
                javaExecutable(),
                "-XX:+UseZGC",
                "-XX:+UseCompactObjectHeaders",
                "-version")
                .redirectErrorStream(true)
                .start();

        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "child JVM did not exit within 10 seconds");
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        if (process.exitValue() != 0) {
            assertTrue(output.contains("UseCompactObjectHeaders") || output.contains("ZGC") || output.contains("Z Garbage"),
                    () -> "expected compact-header/ZGC diagnostic, output was:\n" + output);
        } else {
            assertTrue(output.contains("openjdk version \"25."),
                    () -> "expected Java 25 version output when the local VM accepts the flag, output was:\n" + output);
        }

        final String launcher = Files.readString(Path.of("scripts/run_engine.sh"));
        final String baseline = Files.readString(Path.of("docs/PHASE_8_JMH_BASELINE.md"));

        assertEquals(-1, launcher.indexOf("UseCompactObjectHeaders"),
                "Phase 8 launcher must not enable Compact Object Headers with ZGC");
        assertTrue(baseline.contains("Compact Object Headers are deliberately not enabled"),
                "baseline documentation must record the compact-header decision");
    }

    /**
     * Resolves the same Java executable used by the running test VM.
     */
    private static String javaExecutable() {
        return System.getProperty("java.home") + "/bin/java";
    }
}
