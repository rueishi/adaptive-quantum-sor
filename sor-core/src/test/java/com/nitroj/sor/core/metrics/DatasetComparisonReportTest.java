package com.nitroj.sor.core.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify dataset-backed static-vs-adaptive comparison reports.
 *
 * <p>Role in system: ensures the benchmark comparison profile runs static SOR
 * and adaptive SOR selection over the same feature dataset and writes Markdown
 * evidence.</p>
 *
 * <p>Relationships: executes {@code tools/python-research/scripts/compare_sor_dataset.py} against the
 * generated feature CSV.</p>
 *
 * <p>Lifecycle: executed by Gradle as coverage for the comparison report path.</p>
 *
 * <p>Design intent: this test keeps benchmark comparison from regressing into
 * math-only unit coverage.</p>
 */
final class DatasetComparisonReportTest {
    @TempDir
    private Path tempDir;

    @Test
    void comparisonScriptWritesMarkdownReport() throws IOException, InterruptedException {
        final Path report = tempDir.resolve("report.md");
        final ProcessBuilder builder = new ProcessBuilder(
                "python3",
                "tools/python-research/scripts/compare_sor_dataset.py",
                "--dataset",
                "tools/python-research/examples/sor_notebook_features_large.csv",
                "--output",
                report.toString()
        );
        builder.environment().put("PYTHONPATH", "tools/python-research");
        final Process process = builder.start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("comparison process timed out");
        }

        final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), stderr);
        final String markdown = Files.readString(report);
        assertTrue(markdown.contains("# SOR Static vs Adaptive Comparison Report"));
        assertTrue(markdown.contains("Static SOR chooses"));
        assertTrue(markdown.contains("Adaptive SOR chooses"));
        assertTrue(markdown.contains("Realized improvement bps"));
        assertTrue(markdown.contains("| Average fill bps |"));
    }
}
