package com.nitroj.sor.optnative;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanamaLinkerInventoryTest {
    @Test
    void documentedNativeFunctionsHavePanamaCallersAndNoNativeMethodsInModule() throws Exception {
        assertTrue(Files.readString(Path.of("sor-optimizers-native/src/main/java/com/nitroj/sor/optnative/TacticalOptimizerLinker.java")).contains("tactical_optimize"));
        assertTrue(Files.readString(Path.of("sor-optimizers-native/src/main/java/com/nitroj/sor/optnative/StrategicOptimizerLinker.java")).contains("cudaq_optimize_subset"));
        assertTrue(Files.readString(Path.of("sor-optimizers-native/src/main/java/com/nitroj/sor/optnative/BatchAllocatorLinker.java")).contains("batch_allocate"));
        try (var paths = Files.walk(Path.of("sor-optimizers-native/src/main/java"))) {
            assertFalse(paths.filter(path -> path.toString().endsWith(".java"))
                    .map(PanamaLinkerInventoryTest::read)
                    .anyMatch(source -> source.contains(" native(") || source.contains(" native;")));
        }
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
