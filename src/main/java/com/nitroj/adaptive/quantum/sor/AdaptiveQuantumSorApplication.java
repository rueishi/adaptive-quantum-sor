package com.nitroj.adaptive.quantum.sor;

import com.nitroj.adaptive.quantum.sor.config.ConfigLoader;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsibility: launch the single-process Adaptive Quantum SOR runtime.
 *
 * <p>Role in system: this is the production-style application entry point for
 * Phase 1. It loads configuration, validates the dimensions and runtime mode,
 * and emits the scaffold's startup lifecycle message. Later task cards will
 * wire simulators, policy optimization, routing, API, and lifecycle logging
 * behind this entry point.</p>
 *
 * <p>Relationships: {@link AdaptiveQuantumSorApplication} delegates parsing and validation
 * to {@link ConfigLoader} and {@link SorConfig}. It intentionally does not own
 * simulator or routing behavior yet because those components are introduced by
 * later task cards.</p>
 *
 * <p>Lifecycle: Gradle launches this class through the Application plugin. Tests
 * also call {@link #start(String[])} directly to verify that the entry point is
 * discoverable without forking a separate process.</p>
 *
 * <p>Design intent: the class is named after the process it starts rather than
 * using a generic {@code Main}. That keeps stack traces, Gradle configuration,
 * and future multi-process naming clear.</p>
 */
public final class AdaptiveQuantumSorApplication {

    private AdaptiveQuantumSorApplication() {
    }

    /**
     * Launches the Adaptive Quantum SOR application and exits by propagating startup failures.
     *
     * @param args optional first argument pointing to a configuration file; when
     *             omitted, the classpath default {@code adaptive-quantum-sor.yaml} is used
     * @throws IOException if the configured file cannot be read
     */
    public static void main(final String[] args) throws IOException {
        final StartupResult result = start(args);
        System.out.println(result.lifecycleMessage());
        if (result.apiPort() >= 0) {
            final SorEngineRuntime runtime = SorEngineRuntime.create(result.config(), result.apiPort());
            Runtime.getRuntime().addShutdownHook(new Thread(runtime::close));
            runtime.start();
            System.out.println("ENGINE API listening on http://127.0.0.1:" + runtime.apiPort());
            try {
                runtime.awaitShutdown();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Performs the scaffold-owned startup sequence.
     *
     * <p>The sequence is deliberately small for P1-TC-001: load config, validate
     * it, and create an immutable startup result that later bootstrapping code
     * can extend. Invalid dimensions, unknown keys in strict mode, or malformed
     * runtime mode values fail before any routing work can begin.</p>
     *
     * @param args optional first argument pointing to a YAML config file
     * @return startup result containing the validated config and lifecycle text
     * @throws IOException if a config file path is supplied but cannot be read
     */
    public static StartupResult start(final String[] args) throws IOException {
        final ConfigLoader loader = new ConfigLoader();
        final ParsedArgs parsedArgs = ParsedArgs.parse(args);
        final SorConfig config;
        if (parsedArgs.configPath() == null) {
            config = loader.loadDefault();
        } else {
            config = loader.load(parsedArgs.configPath());
        }

        return new StartupResult(
                config,
                "STARTUP initialized engine context with mode="
                        + config.runtimeMode()
                        + ", instruments="
                        + config.instrumentCount()
                        + ", venues="
                        + config.venueCount(),
                parsedArgs.apiPort()
        );
    }

    private record ParsedArgs(Path configPath, int apiPort) {
        private static ParsedArgs parse(final String[] args) {
            Path configPath = null;
            int apiPort = -1;
            if (args != null) {
                for (String arg : args) {
                    if (arg == null || arg.isBlank()) {
                        continue;
                    }
                    if (arg.startsWith("--api-port=")) {
                        apiPort = Integer.parseInt(arg.substring("--api-port=".length()));
                    } else if ("--api".equals(arg)) {
                        apiPort = 8080;
                    } else if (configPath == null) {
                        configPath = Path.of(arg);
                    } else {
                        throw new IllegalArgumentException("unexpected argument: " + arg);
                    }
                }
            }
            return new ParsedArgs(configPath, apiPort);
        }
    }

    /**
     * Immutable value produced by scaffold startup.
     *
     * <p>Responsibility: carry the validated configuration and human-readable
     * startup lifecycle event created by {@link #start(String[])}. Later tasks
     * may replace or extend this with the full engine context, but this minimal
     * structure gives tests a stable output contract now.</p>
     */
    public record StartupResult(SorConfig config, String lifecycleMessage, int apiPort) {
        /**
         * Validates record construction so startup tests catch broken wiring.
         *
         * @param config validated application configuration
         * @param lifecycleMessage startup lifecycle event text
         */
        public StartupResult {
            if (config == null) {
                throw new IllegalArgumentException("config must not be null");
            }
            if (lifecycleMessage == null || lifecycleMessage.isBlank()) {
                throw new IllegalArgumentException("lifecycleMessage must not be blank");
            }
        }

        public StartupResult(final SorConfig config, final String lifecycleMessage) {
            this(config, lifecycleMessage, -1);
        }
    }
}
