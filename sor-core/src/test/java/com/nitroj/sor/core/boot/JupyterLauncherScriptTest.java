package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the notebook launcher starts the canonical server application. */
final class JupyterLauncherScriptTest {
    @Test
    void startupScriptOpensNotebookPanelsAgainstSorServer() throws Exception {
        final String script = Files.readString(Path.of("scripts/start-jupyter-lab.sh"));

        assertTrue(script.contains(":sor-test-server:runNotebookApi"));
        assertTrue(script.contains("--http-control-port"));
        assertTrue(script.contains(":sor-test-server:classes"));
        assertTrue(script.contains("notebooks/submit_parent_order.ipynb"));
        assertTrue(script.contains("notebooks/live_stats_monitor.ipynb"));
        assertTrue(script.contains("notebooks/scenario_runner.ipynb"));
        assertTrue(script.contains("three notebook panels"));
        assertTrue(script.contains("PYTHONPATH"));
        assertTrue(script.contains("tools/python-research/examples/sor_notebook_features_large.csv"));
    }
}
