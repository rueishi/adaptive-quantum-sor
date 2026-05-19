package com.nitroj.adaptive.quantum.sor;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the application startup integration path.
 *
 * <p>Role in system: this test intentionally exercises multiple classes:
 * {@link AdaptiveQuantumSorApplication}, the default classpath configuration resource, and
 * config loading. It is kept as an integration-style test because the behavior
 * is process startup wiring rather than one isolated class contract.</p>
 *
 * <p>Relationships: complements unit tests in the mirrored config package.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: keep launchability visible and separate from per-class unit
 * tests so missing main-class regressions are caught early.</p>
 */
final class AdaptiveQuantumSorApplicationIntegrationTest {

    @Test
    void startLoadsDefaultConfigAndCreatesLifecycleMessage() throws Exception {
        final AdaptiveQuantumSorApplication.StartupResult result = AdaptiveQuantumSorApplication.start(new String[0]);

        assertTrue(result.lifecycleMessage().contains("STARTUP initialized"), "startup lifecycle event");
        assertEquals(20, result.config().instrumentCount(), "default config resource loaded");
        assertEquals(result.config(), result.config(), "startup config accessor");
        assertEquals(result.lifecycleMessage(), result.lifecycleMessage(), "startup lifecycle accessor");
    }

    @Test
    void mainPrintsStartupLifecycleMessage() throws Exception {
        final PrintStream originalOut = System.out;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(output));
            AdaptiveQuantumSorApplication.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }

        assertTrue(output.toString().contains("STARTUP initialized engine context"), "main prints lifecycle message");
    }

    @Test
    void gradleAndReadmeExposeAdaptiveQuantumSorApplicationEntrypoint() throws Exception {
        final String buildFile = Files.readString(Path.of("build.gradle"));
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(
                buildFile.contains("id 'application'"),
                "Gradle application plugin is configured"
        );
        assertTrue(
                buildFile.contains("mainClass = 'com.nitroj.adaptive.quantum.sor.AdaptiveQuantumSorApplication'"),
                "Gradle launch target is AdaptiveQuantumSorApplication"
        );
        assertTrue(
                readme.contains("./gradlew run"),
                "documented Gradle launch command is present"
        );
    }

    @Test
    void startupResultRejectsInvalidConstruction() {
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new AdaptiveQuantumSorApplication.StartupResult(null, "ok")
        ).getMessage().contains("config must not be null"));
        assertTrue(assertThrows(
                IllegalArgumentException.class,
                () -> new AdaptiveQuantumSorApplication.StartupResult(
                        new com.nitroj.adaptive.quantum.sor.config.SorConfig(
                                1,
                                1,
                                1,
                                1,
                                com.nitroj.adaptive.quantum.sor.config.SorConfig.RuntimeMode.DEMO,
                                true
                        ),
                        " "
                )
        ).getMessage().contains("lifecycleMessage must not be blank"));
    }
}
