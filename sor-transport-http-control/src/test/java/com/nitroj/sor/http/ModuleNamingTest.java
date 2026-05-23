package com.nitroj.sor.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleNamingTest {
    @Test
    void readmeDocumentsControlPlaneOnly() throws Exception {
        final String readme = Files.readString(Path.of("sor-transport-http-control/README.md"));

        assertTrue(readme.contains("research and ops only"));
        assertTrue(readme.contains("is not the production order intake path"));
    }
}
