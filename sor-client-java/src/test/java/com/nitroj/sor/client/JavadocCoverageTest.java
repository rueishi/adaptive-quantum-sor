package com.nitroj.sor.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavadocCoverageTest {
    @Test
    void publicSdkTypesHaveSourceJavadocs() throws Exception {
        for (Path source : Files.walk(Path.of("sor-client-java/src/main/java/com/nitroj/sor/client"))
                .filter(path -> path.toString().endsWith(".java"))
                .toList()) {
            final String text = Files.readString(source);
            assertTrue(text.contains("/**"), source.toString());
        }
    }
}
