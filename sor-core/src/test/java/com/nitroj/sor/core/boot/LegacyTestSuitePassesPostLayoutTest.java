package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies that representative core implementation and tests
 * still live under the `sor-core` module after the P8-02 layout move.
 *
 * <p>Role in system: P8-02 is meant to be mechanical. This test checks that the
 * representative core sources moved without losing the test/JMH coverage
 * expected by later Phase 8 cards.</p>
 *
 * <p>Relationships: full behavioral confidence still comes from
 * `:sor-core:check`; this test gives a fast structural failure if the source
 * tree was not moved as specified.</p>
 *
 * <p>Lifecycle: runs inside the moved `sor-core` test suite.</p>
 *
 * <p>Design intent: make the old-to-new source location explicit for the next
 * engineer reading Phase 8 diffs.</p>
 */
class LegacyTestSuitePassesPostLayoutTest {
    /**
     * Checks representative production, test, and JMH files under the new
     * module root.
     */
    @Test
    void coreSourceSetsMovedIntoSorCore() {
        assertTrue(Files.isRegularFile(Path.of("sor-core/src/main/java/com/nitroj/sor/core/SorEngineImpl.java")));
        assertTrue(Files.isRegularFile(Path.of("sor-test-server/src/test/java/com/nitroj/sor/testserver/e2e/SorEndToEndTest.java")));
        assertTrue(Files.isRegularFile(Path.of("sor-core/src/jmh/java/com/nitroj/sor/core/benchmark/PolicyDrivenSorJmhBenchmark.java")));
        assertFalse(Files.exists(Path.of("sor-test-server/src/test/java/com/nitroj/adaptive/quantum/sor/e2e/SorEndToEndTest.java")));
        assertFalse(Files.exists(Path.of("sor-core/src/jmh/java/com/nitroj/adaptive/quantum/sor/benchmark/PolicyDrivenSorJmhBenchmark.java")));
    }
}
