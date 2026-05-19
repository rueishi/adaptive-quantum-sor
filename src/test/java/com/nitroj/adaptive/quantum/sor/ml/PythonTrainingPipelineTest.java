package com.nitroj.adaptive.quantum.sor.ml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 4 Python training pipeline.
 *
 * <p>Role in system: covers P4-TC-002 by proving the training job runs,
 * produces model artifacts, writes validation metrics, and handles failures
 * without touching the Java SOR runtime.</p>
 *
 * <p>Relationships: invokes {@code python/train_models.py} against a
 * schema-versioned CSV dataset.</p>
 *
 * <p>Lifecycle: executed by Gradle as a process-level integration test for the
 * offline training path.</p>
 *
 * <p>Design intent: pure-stdlib Python keeps this test deterministic in local
 * and CI environments.</p>
 */
final class PythonTrainingPipelineTest {
    @TempDir
    private Path tempDir;

    @Test
    void trainingJobRunsAndProducesArtifactsAndMetrics() throws IOException, InterruptedException {
        final Path dataset = dataset();
        final Path outputDir = tempDir.resolve("artifacts");

        final ProcessResult result = runPython(dataset, outputDir);

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(Files.isRegularFile(outputDir.resolve("fill_probability.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("toxicity.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("slippage.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("regime.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("predictions.csv")));
        assertTrue(Files.isRegularFile(outputDir.resolve("validation_metrics.csv")));
        assertTrue(Files.readString(outputDir.resolve("model_metadata.properties")).contains("featureSchemaVersion=feature-schema-v1"));
        assertTrue(Files.readString(outputDir.resolve("validation_metrics.csv")).contains("row_count,2"));
    }

    @Test
    void trainingFailureHandled() throws IOException, InterruptedException {
        final Path badDataset = tempDir.resolve("bad.csv");
        Files.writeString(badDataset, "schema_version,label_fill_bps\nfeature-schema-v1,1\n", StandardCharsets.UTF_8);

        final ProcessResult result = runPython(badDataset, tempDir.resolve("bad-artifacts"));

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("training failed: dataset missing required columns"));
        assertFalse(Files.exists(tempDir.resolve("bad-artifacts").resolve("model_metadata.properties")));
    }

    private Path dataset() throws IOException {
        final Path dataset = tempDir.resolve("training.csv");
        Files.write(dataset, List.of(
                FeatureSchema.csvHeader(),
                "feature-schema-v1,11,77,3,0,0,0,100,101,1,1000,900,1000,8000,100,50,25,2,0,10000,25,100,0",
                "feature-schema-v1,11,77,3,0,1,0,99,102,3,800,700,2000,4000,500,2000,250,2,0,0,250,500,0"
        ), StandardCharsets.UTF_8);
        return dataset;
    }

    private static ProcessResult runPython(final Path dataset, final Path outputDir) throws IOException, InterruptedException {
        final Process process = new ProcessBuilder(
                "python3",
                "python/train_models.py",
                "--dataset",
                dataset.toString(),
                "--output-dir",
                outputDir.toString()
        ).start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("training process timed out");
        }
        return new ProcessResult(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
