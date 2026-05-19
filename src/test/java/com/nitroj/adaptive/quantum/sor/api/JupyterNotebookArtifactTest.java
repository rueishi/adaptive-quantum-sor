package com.nitroj.adaptive.quantum.sor.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify notebook client artifacts for P1-TC-024.
 *
 * <p>Role in system: ensures the Jupyter notebooks reference the required API
 * endpoints for order submission, status, scenario runs, stats, and policy.</p>
 *
 * <p>Relationships: validates files under `notebooks/` that call
 * {@link SorHttpApiServer} endpoints.</p>
 *
 * <p>Lifecycle: executed by Gradle as artifact coverage for notebook task cards.</p>
 *
 * <p>Design intent: artifact tests give CI confidence without launching a real
 * notebook kernel.</p>
 */
final class JupyterNotebookArtifactTest {
    @Test
    void notebooksContainRequiredApiWorkflows() throws Exception {
        final String submit = Files.readString(Path.of("notebooks/submit_parent_order.ipynb"));
        final String stats = Files.readString(Path.of("notebooks/live_stats_monitor.ipynb"));
        final String scenario = Files.readString(Path.of("notebooks/scenario_runner.ipynb"));
        final String notebookReadme = Files.readString(Path.of("notebooks/README.md"));
        final String pythonReadme = Files.readString(Path.of("python/README.md"));
        final String client = Files.readString(Path.of("python/adaptive_quantum_sor/client.py"));
        final String dataframe = Files.readString(Path.of("python/adaptive_quantum_sor/dataframe.py"));

        assertTrue(submit.contains("requests.post"));
        assertTrue(submit.contains("/orders"));
        assertTrue(stats.contains("/stats/current"));
        assertTrue(stats.contains("/policy/current"));
        assertTrue(stats.contains("pandas"));
        assertTrue(stats.contains("sor-kpi-grid"));
        assertTrue(stats.contains("sor-bar-panel"));
        assertTrue(stats.contains("SorNotebookClient"));
        assertTrue(scenario.contains("reset_scenario"));
        assertTrue(scenario.contains("run_scenario"));
        assertTrue(scenario.contains("scenario_summary_dataframe"));
        assertTrue(scenario.contains("scenario_events_dataframe"));
        assertTrue(notebookReadme.contains("# Jupyter Notebook User Guide"));
        assertTrue(notebookReadme.contains("## Start JupyterLab"));
        assertTrue(notebookReadme.contains("scripts/start-jupyter-lab.sh"));
        assertTrue(notebookReadme.contains("## Live Order Testing"));
        assertTrue(notebookReadme.contains("## Scenario Testing"));
        assertTrue(notebookReadme.contains("## Scenario Catalog Library And Commands"));
        assertTrue(notebookReadme.contains("from adaptive_quantum_sor import load_scenarios"));
        assertTrue(notebookReadme.contains("python3 -m adaptive_quantum_sor.scenario_catalog list"));
        assertTrue(notebookReadme.contains("## Scenario Replay Flow"));
        assertTrue(notebookReadme.contains("### `submit_parent_order.ipynb`"));
        assertTrue(notebookReadme.contains("### `live_stats_monitor.ipynb`"));
        assertTrue(notebookReadme.contains("Expected result:"));
        assertTrue(notebookReadme.contains("## Research Dataset Inspection"));
        assertTrue(notebookReadme.contains("## Troubleshooting"));
        assertTrue(pythonReadme.contains("# Python Research Helpers"));
        assertTrue(pythonReadme.contains("notebooks/README.md"));
        assertTrue(pythonReadme.contains("SorNotebookClient"));
        assertTrue(pythonReadme.contains("read_feature_dataframe"));
        assertTrue(pythonReadme.contains("write_model_predictions_artifact"));
        assertTrue(pythonReadme.contains("python/examples/sor_notebook_features_large.csv"));
        assertTrue(pythonReadme.contains("## Train Models"));
        assertTrue(pythonReadme.contains("python3 python/train_models.py"));
        assertTrue(pythonReadme.contains("## Create A Java-Importable Model Artifact"));
        assertTrue(client.contains("submit_orders_dataframe"));
        assertTrue(client.contains("stats_dataframe"));
        assertTrue(client.contains("reset_scenario"));
        assertTrue(client.contains("run_scenario"));
        assertTrue(dataframe.contains("validate_feature_dataframe"));
        assertTrue(dataframe.contains("write_model_predictions_artifact"));
    }
}
