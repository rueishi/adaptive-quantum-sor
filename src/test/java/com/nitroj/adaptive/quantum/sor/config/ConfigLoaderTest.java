package com.nitroj.adaptive.quantum.sor.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link ConfigLoader}.
 *
 * <p>Role in system: verifies startup-only parsing behavior, including
 * positive config loads, strict unknown-key failures, malformed scalar
 * exceptions, and file path handling.</p>
 *
 * <p>Relationships: constructs {@link SorConfig} as the loader output but keeps
 * constructor-specific behavior in {@link SorConfigTest}.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: keep parser edge cases local to the parser test so config
 * failures remain easy to diagnose.</p>
 */
final class ConfigLoaderTest {

    @Test
    void loadStringParsesValidConfig() {
        final SorConfig config = new ConfigLoader().loadString("""
                adaptiveQuantumSor:
                  instruments: 2
                  venues: 3
                  regimes: 4
                  urgencies: 5
                runtime:
                  mode: demo
                config:
                  strictUnknownKeys: true
                """);

        assertEquals(2, config.instrumentCount());
        assertEquals(3, config.venueCount());
        assertEquals(4, config.regimeCount());
        assertEquals(5, config.urgencyCount());
        assertEquals(SorConfig.RuntimeMode.DEMO, config.runtimeMode());
        assertTrue(config.strictUnknownKeys());
    }

    @Test
    void loadDefaultReadsClasspathResource() throws Exception {
        final SorConfig config = new ConfigLoader().loadDefault();

        assertEquals(20, config.instrumentCount());
        assertEquals(100, config.venueCount());
    }

    @Test
    void loadReadsExplicitPath() throws Exception {
        final Path configPath = Files.createTempFile("adaptive-quantum-sor", ".yaml");
        Files.writeString(configPath, """
                adaptiveQuantumSor:
                  instruments: 7
                  venues: 8
                  regimes: 2
                  urgencies: 3
                runtime:
                  mode: strict
                """);

        final SorConfig config = new ConfigLoader().load(configPath);

        assertEquals(7, config.instrumentCount());
        assertEquals(8, config.venueCount());
        assertEquals(SorConfig.RuntimeMode.STRICT, config.runtimeMode());
    }

    @Test
    void loadRejectsMissingOrNullPath() {
        final IOException missing = assertThrows(
                IOException.class,
                () -> new ConfigLoader().load(Path.of("/tmp/does-not-exist-adaptive-quantum-sor.yaml"))
        );
        assertTrue(missing.getMessage().contains("config file not found"));

        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().load(null)
        ).getMessage().contains("path must not be null"));
    }

    @Test
    void loadStringRejectsInvalidDimensionsAndMalformedScalars() {
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("")
        ).getMessage().contains("config content must not be blank"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("not valid")
        ).getMessage().contains("invalid config line"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("""
                        adaptiveQuantumSor:
                          instruments: 0
                        """)
        ).getMessage().contains("instrumentCount must be positive"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("""
                        adaptiveQuantumSor:
                          instruments: nope
                        """)
        ).getMessage().contains("adaptiveQuantumSor.instruments must be an integer"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("""
                        config:
                          strictUnknownKeys: maybe
                        """)
        ).getMessage().contains("config.strictUnknownKeys must be true or false"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("""
                        runtime:
                          mode: turbo
                        """)
        ).getMessage().contains("runtime.mode must be demo or strict"));
    }

    @Test
    void unknownKeysHonorStrictMode() {
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new ConfigLoader().loadString("""
                        adaptiveQuantumSor:
                          instruments: 1
                          surprise: true
                        """)
        ).getMessage().contains("unknown config keys"));

        final SorConfig permissive = new ConfigLoader().loadString("""
                config:
                  strictUnknownKeys: false
                adaptiveQuantumSor:
                  instruments: 1
                  surprise: true
                """);
        assertEquals(1, permissive.instrumentCount());
    }
}
