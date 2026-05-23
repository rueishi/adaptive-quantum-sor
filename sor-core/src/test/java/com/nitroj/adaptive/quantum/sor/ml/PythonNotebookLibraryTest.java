package com.nitroj.adaptive.quantum.sor.ml;

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
 * Responsibility: verify the Python notebook helper library.
 *
 * <p>Role in system: ensures Jupyter users can import the Python package,
 * validate feature records, write Java-compatible model artifacts, and receive
 * a clear pandas installation message when DataFrame helpers are used without
 * pandas installed.</p>
 *
 * <p>Relationships: executes modules under {@code tools/python-research/adaptive_quantum_sor_research} through the
 * local Python interpreter.</p>
 *
 * <p>Lifecycle: executed by Gradle as artifact coverage for the notebook/Python
 * control-plane helper package.</p>
 *
 * <p>Design intent: the test avoids network dependency installation while still
 * covering the package contract used from notebooks.</p>
 */
final class PythonNotebookLibraryTest {
    @TempDir
    private Path tempDir;

    @Test
    void notebookLibraryWritesDatasetAndModelArtifact() throws IOException, InterruptedException {
        final Path script = tempDir.resolve("exercise_adaptive_quantum_sor.py");
        Files.writeString(script, """
                from pathlib import Path
                from adaptive_quantum_sor_research import FEATURE_SCHEMA_VERSION, write_feature_dataset, write_model_predictions_artifact

                root = Path(r'%s')
                rows = [{
                    'schema_version': FEATURE_SCHEMA_VERSION,
                    'event_id': 1,
                    'parent_order_id': 2,
                    'policy_version': 3,
                    'instrument_id': 0,
                    'venue_id': 1,
                    'regime_id': 0,
                    'bid_price_ticks': 100,
                    'ask_price_ticks': 101,
                    'spread_ticks': 1,
                    'bid_qty': 1000,
                    'ask_qty': 900,
                    'latency_nanos': 1000,
                    'fill_probability_bps': 7000,
                    'toxicity_bps': 100,
                    'reject_rate_bps': 50,
                    'slippage_bps': 25,
                    'route_child_order_count': 1,
                    'route_residual_qty': 0,
                    'label_fill_bps': 10000,
                    'label_slippage_bps': 25,
                    'label_toxicity_bps': 100,
                    'label_regime_id': 0,
                }]
                write_feature_dataset(rows, root / 'training.csv')
                write_model_predictions_artifact([{
                    'instrument_id': 0,
                    'venue_id': 1,
                    'regime_id': 0,
                    'venue_score_bps': 6500,
                }], root / 'artifact')
                print((root / 'training.csv').read_text().splitlines()[0])
                print((root / 'artifact' / 'model_metadata.properties').read_text())
                """.formatted(tempDir.toString().replace("\\", "\\\\")), StandardCharsets.UTF_8);

        final ProcessResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(result.stdout().contains("schema_version,event_id,parent_order_id"));
        assertTrue(result.stdout().contains("featureSchemaVersion=feature-schema-v1"));
        assertTrue(result.stdout().contains("predictionChecksumSha256="));
    }

    @Test
    void dataframeHelpersExplainMissingPandasDependency() throws IOException, InterruptedException {
        final Path script = tempDir.resolve("missing_pandas.py");
        Files.writeString(script, """
                from adaptive_quantum_sor_research import records_to_dataframe
                try:
                    records_to_dataframe([])
                except ModuleNotFoundError as exc:
                    print(str(exc))
                """, StandardCharsets.UTF_8);

        final ProcessResult result = runPython(script);

        assertEquals(0, result.exitCode(), result.stderr());
        assertTrue(result.stdout().contains("pandas is required for DataFrame helpers"));
    }

    @Test
    void exposesResetScenarioAndRunScenarioHelpers() throws IOException {
        final String client = Files.readString(Path.of("tools/notebook-helpers/adaptive_quantum_sor_notebooks/client.py"), StandardCharsets.UTF_8);

        assertTrue(client.contains("def reset_scenario"));
        assertTrue(client.contains("/scenario/reset"));
        assertTrue(client.contains("def run_scenario"));
        assertTrue(client.contains("/scenario/run"));
        assertTrue(client.contains("simulatorGeneratedOrders"));
        assertTrue(client.contains("submitMode"));
        assertTrue(client.contains("clientOrderRef"));
        assertTrue(client.contains("scenario_summary_dataframe"));
        assertTrue(client.contains("scenario_events_dataframe"));
    }

    private static ProcessResult runPython(final Path script) throws IOException, InterruptedException {
        final ProcessBuilder builder = new ProcessBuilder("python3", script.toString())
                .directory(Path.of(".").toFile());
        builder.environment().put("PYTHONPATH", "tools/python-research");
        final Process process = builder.start();
        process.getOutputStream().close();
        process.getInputStream();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("python process timed out");
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
