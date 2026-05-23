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
        final String pythonReadme = Files.readString(Path.of("tools/python-research/README.md"));
        final String client = Files.readString(Path.of("tools/notebook-helpers/adaptive_quantum_sor_notebooks/client.py"));
        final String dataframe = Files.readString(Path.of("tools/python-research/adaptive_quantum_sor_research/dataframe.py"));
        final String submitReport = Files.readString(Path.of("tools/notebook-helpers/adaptive_quantum_sor_notebooks/submit_order_report.py"));
        final String liveMonitor = Files.readString(Path.of("tools/notebook-helpers/adaptive_quantum_sor_notebooks/live_monitor.py"));
        final String scenarioReport = Files.readString(Path.of("tools/notebook-helpers/adaptive_quantum_sor_notebooks/scenario_report.py"));

        assertTrue(submit.contains("SubmitParentOrderApp"));
        assertTrue(submit.contains("app.display_controls"));
        assertTrue(submit.contains("app.submit_and_display"));
        assertTrue(submitReport.contains("SorNotebookClient"));
        assertTrue(submitReport.contains("submit_order"));
        assertTrue(submitReport.contains("order_status"));
        assertTrue(submitReport.contains("/orders"));
        assertTrue(submitReport.contains("Widget Control Panel"));
        assertTrue(submitReport.contains("read_inputs"));
        assertTrue(submitReport.contains("Parent Order Submission Report"));
        assertTrue(submitReport.contains("Submission Manifest"));
        assertTrue(submitReport.contains("Result List"));
        assertTrue(stats.contains("adaptive_quantum_sor_notebooks.live_monitor"));
        assertTrue(stats.contains("LiveStatsDashboard"));
        assertTrue(stats.contains("dashboard.display"));
        assertTrue(liveMonitor.contains("stats_dataframe"));
        assertTrue(liveMonitor.contains("policy_dataframe"));
        assertTrue(liveMonitor.contains("order_summary_frames"));
        assertTrue(liveMonitor.contains("Start dashboard"));
        assertTrue(liveMonitor.contains("Reset"));
        assertTrue(liveMonitor.contains("Stop"));
        assertTrue(liveMonitor.contains("Order Flow"));
        assertTrue(liveMonitor.contains("Fill Progress"));
        assertTrue(liveMonitor.contains("Filled Quantity By Venue"));
        assertTrue(liveMonitor.contains("Instrument-Venue Routes"));
        assertTrue(liveMonitor.contains("Stats And Policy Snapshot"));
        assertTrue(scenario.contains("adaptive_quantum_sor_notebooks.scenario_report"));
        assertTrue(scenario.contains("ScenarioReportApp"));
        assertTrue(scenario.contains("display_controls"));
        assertTrue(scenario.contains("display_planned_run"));
        assertTrue(scenario.contains("Execute"));
        assertTrue(scenario.contains("Scenario Execution Report"));
        assertTrue(scenarioReport.contains("reset_scenario"));
        assertTrue(scenarioReport.contains("run_scenario"));
        assertTrue(scenarioReport.contains("parent_order"));
        assertTrue(scenarioReport.contains("atTick"));
        assertTrue(scenarioReport.contains("submitMode"));
        assertTrue(scenarioReport.contains("clientOrderRef"));
        assertTrue(scenarioReport.contains("scenario_summary_dataframe"));
        assertTrue(scenarioReport.contains("scenario_events_dataframe"));
        assertTrue(scenarioReport.contains("Widget Control Panel"));
        assertTrue(scenarioReport.contains("Update setup"));
        assertTrue(scenarioReport.contains("Execute"));
        assertTrue(scenarioReport.contains("Run Manifest"));
        assertTrue(scenarioReport.contains("Result List"));
        assertTrue(scenarioReport.contains("Venue Fill Breakdown"));
        assertTrue(scenarioReport.contains("Lifecycle Event Log"));
        assertTrue(notebookReadme.contains("# Jupyter Notebook User Guide"));
        assertTrue(notebookReadme.contains("## Start JupyterLab"));
        assertTrue(notebookReadme.contains("scripts/start-jupyter-lab.sh"));
        assertTrue(notebookReadme.contains("## Live Order Testing"));
        assertTrue(notebookReadme.contains("## Scenario Testing"));
        assertTrue(notebookReadme.contains("## Scenario Catalog Library And Commands"));
        assertTrue(notebookReadme.contains("from adaptive_quantum_sor_research import load_scenarios"));
        assertTrue(notebookReadme.contains("python3 -m adaptive_quantum_sor_research.scenario_catalog list"));
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
        assertTrue(pythonReadme.contains("tools/python-research/examples/sor_notebook_features_large.csv"));
        assertTrue(pythonReadme.contains("## Train Models"));
        assertTrue(pythonReadme.contains("python3 tools/python-research/scripts/train_models.py"));
        assertTrue(pythonReadme.contains("## Create A Java-Importable Model Artifact"));
        assertTrue(client.contains("submit_orders_dataframe"));
        assertTrue(client.contains("stats_dataframe"));
        assertTrue(client.contains("reset_scenario"));
        assertTrue(client.contains("run_scenario"));
        assertTrue(dataframe.contains("validate_feature_dataframe"));
        assertTrue(dataframe.contains("write_model_predictions_artifact"));
    }
}
