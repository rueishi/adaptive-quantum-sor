package com.nitroj.sor.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Java SDK publication metadata is suitable for Maven Central-style publishing.
 *
 * <p>Run with client tests before release packaging changes.</p>
 */
class MavenCentralPublishDryRunTest {
    @Test
    void buildFileContainsPublicationMetadataRequiredByCentral() throws Exception {
        final String build = Files.readString(Path.of("sor-client-java/build.gradle"));
        for (String required : new String[]{
                "maven-publish",
                "signing",
                "artifactId = 'sor-client-java'",
                "description =",
                "licenses",
                "developers",
                "scm"
        }) {
            assertTrue(build.contains(required), required);
        }
    }
}
