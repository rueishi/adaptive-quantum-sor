package com.nitroj.sor.optnative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingLibraryDiagnosticTest {
    @TempDir Path dir;

    @Test
    void missingLibraryNamesAttemptedPath() {
        final String old = System.getProperty("sor.native.lib.dir");
        System.setProperty("sor.native.lib.dir", dir.toString());
        try {
            final IllegalStateException ex = assertThrows(IllegalStateException.class, TacticalOptimizerLinker::new);
            assertTrue(ex.getMessage().contains("libtactical_optimizer_api.a"));
            assertTrue(ex.getMessage().contains(dir.toString()));
        } finally {
            if (old == null) {
                System.clearProperty("sor.native.lib.dir");
            } else {
                System.setProperty("sor.native.lib.dir", old);
            }
        }
    }
}
