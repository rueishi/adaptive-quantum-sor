package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.SorEngineRuntime;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Responsibility: legacy notebook entry point that starts the shared SOR engine runtime.
 *
 * <p>Role in system: JupyterLab startup scripts need a long-running control
 * plane with an active policy, stats endpoint, order endpoint, and event stream.</p>
 *
 * <p>Relationships: delegates engine ownership to {@link SorEngineRuntime}.</p>
 *
 * <p>Lifecycle: run by `scripts/start-jupyter-lab.sh` and terminated when the
 * script exits.</p>
 *
 * <p>Design intent: preserve compatibility while the canonical application
 * entry point remains {@code AdaptiveQuantumSorApplication --api-port=...}.</p>
 */
public final class NotebookDemoApiLauncher {
    private NotebookDemoApiLauncher() {
    }

    public static void main(final String[] args) throws Exception {
        final int port = args == null || args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        final SorEngineRuntime runtime = SorEngineRuntime.create(new SorConfig(3, 5, 4, 2, SorConfig.RuntimeMode.DEMO, true), port);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));
        runtime.start();
        System.out.println("Notebook engine API listening on http://127.0.0.1:" + runtime.apiPort());
        new CountDownLatch(1).await();
    }

    public static SorHttpApiServer createServer(final int port) throws IOException {
        final SorEngineRuntime runtime = SorEngineRuntime.create(new SorConfig(3, 5, 4, 2, SorConfig.RuntimeMode.DEMO, true), port);
        return runtime.apiServer();
    }
}
