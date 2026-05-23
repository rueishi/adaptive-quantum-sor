package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the committed Phase 8 JMH baseline has the metadata
 * and benchmark rows required by the regression gate.
 *
 * <p>Role in system: P8-01 establishes a fixed benchmark floor. Later cards
 * may update the numbers deliberately, but they must preserve this parseable
 * document structure.</p>
 *
 * <p>Relationships: the Gradle `jmhRegressionCheck` task reads the benchmark
 * table validated here.</p>
 *
 * <p>Lifecycle: executed as a normal unit test after documentation edits.</p>
 *
 * <p>Design intent: keep the baseline human-readable while still making the
 * key benchmark row machine-checkable.</p>
 */
class JmhBaselineFileFormatTest {
    private static final Path BASELINE = Path.of("docs/testing/PHASE_8_JMH_BASELINE.md");

    /**
     * Confirms the baseline document includes all required sections and the
     * strict route benchmark row used by the Gradle gate.
     *
     * @throws IOException if the baseline file cannot be read
     */
    @Test
    void baselineContainsEnvironmentMetadataAndBenchmarkRows() throws IOException {
        final String markdown = Files.readString(BASELINE);

        assertContains(markdown, "## Environment Metadata");
        assertContains(markdown, "## ZGC And Compact Object Headers");
        assertContains(markdown, "## Benchmark Baseline");
        assertContains(markdown, "## Hot-Path Allocation");
        assertContains(markdown, "## Hot-Path Latency");
        assertContains(markdown, "## Optimizer Cycle Latency");
        assertContains(markdown, "OpenJDK 25");
        assertContains(markdown, "Generational ZGC");
        assertContains(markdown,
                "| com.nitroj.adaptive.quantum.sor.benchmark.PolicyDrivenSorJmhBenchmark.strictRouteInto | 70.304 |");
    }

    /**
     * Small assertion helper that includes the missing fragment in the failure.
     */
    private static void assertContains(final String text, final String expected) {
        assertTrue(text.contains(expected), () -> "missing expected baseline fragment: " + expected);
    }
}
