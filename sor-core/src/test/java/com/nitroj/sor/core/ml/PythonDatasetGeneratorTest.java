package com.nitroj.sor.core.ml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the large SOR feature dataset generator.
 *
 * <p>Role in system: ensures comparison/research datasets can be regenerated
 * deterministically at different sizes without hand-editing CSV fixtures.</p>
 *
 * <p>Relationships: executes {@code tools/python-research/scripts/generate_sor_dataset.py} and checks
 * the generated schema-compatible CSV shape.</p>
 *
 * <p>Lifecycle: executed by Gradle with Python artifact tests.</p>
 *
 * <p>Design intent: generated data supports broader static-vs-adaptive
 * comparison scenarios while keeping the repository fixture reproducible.</p>
 */
final class PythonDatasetGeneratorTest {
    @TempDir
    private Path tempDir;

    @Test
    void generatorWritesRequestedRowsWithFeatureSchema() throws IOException, InterruptedException {
        final Path output = tempDir.resolve("generated.csv");
        final ProcessBuilder builder = new ProcessBuilder(
                "python3",
                "tools/python-research/scripts/generate_sor_dataset.py",
                "--rows",
                "256",
                "--output",
                output.toString()
        );
        builder.environment().put("PYTHONPATH", "tools/python-research");
        final Process process = builder.start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("dataset generator timed out");
        }

        final String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), stderr);
        final List<String> lines = Files.readAllLines(output);
        assertEquals(257, lines.size());
        assertTrue(lines.get(0).startsWith("schema_version,event_id,parent_order_id"));
        assertTrue(lines.get(1).startsWith("feature-schema-v1,"));
    }
}
