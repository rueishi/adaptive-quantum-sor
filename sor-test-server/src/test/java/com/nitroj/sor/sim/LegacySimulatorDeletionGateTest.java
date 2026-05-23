package com.nitroj.sor.sim;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifies the legacy simulator package has been deleted after simulator migration. */
final class LegacySimulatorDeletionGateTest {
    @Test
    void legacyPackageIsDeleted() {
        assertFalse(Files.exists(Path.of("sor-core/src/main/java/com/nitroj/adaptive/quantum/sor/sim")));
        assertFalse(Files.exists(Path.of("sor-core/src/test/java/com/nitroj/adaptive/quantum/sor/sim")));
    }
}
