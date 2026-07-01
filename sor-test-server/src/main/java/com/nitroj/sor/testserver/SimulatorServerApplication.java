package com.nitroj.sor.testserver;

import com.nitroj.sor.api.SorConfig;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEngineBuilder;
import com.nitroj.sor.http.HttpControlPlaneServer;
import com.nitroj.sor.obs.SorObservability;
import com.nitroj.sor.testkit.sim.adapters.InMemoryPersistence;
import com.nitroj.sor.testkit.sim.adapters.ManualClock;
import com.nitroj.sor.testkit.sim.adapters.SimulatedMarketDataSource;
import com.nitroj.sor.testkit.sim.adapters.SimulatedRiskProvider;
import com.nitroj.sor.testkit.sim.adapters.SimulatedVenueAdapter;
import com.nitroj.sor.transport.aeron.AeronSorServer;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Responsibility: assemble and run the simulator-backed sample SOR server.
 *
 * <p>Role in system: this is the launchable `sor-test-server` application. It
 * builds a {@link SorEngine} with deterministic testkit SPI implementations,
 * starts the supported {@link HttpControlPlaneServer}, and optionally exposes
 * the Aeron transport wrapper for out-of-process client testing.</p>
 *
 * <p>Relationships: depends on public {@code sor-api} engine contracts,
 * {@code sor-testkit} simulated market/risk/venue/persistence adapters,
 * {@code sor-transport-http-control} for HTTP health/readiness/control, and
 * {@code sor-transport-aeron} for remote transport smoke coverage.</p>
 *
 * <p>Lifecycle: {@link #main(String[])} parses command-line options, starts a
 * {@link ServerRuntime}, registers a shutdown hook, and blocks the main thread.
 * Tests can call {@link #start(Options)} directly with an ephemeral HTTP port
 * and close the returned runtime.</p>
 *
 * <p>Design intent: keep the runnable sample server separate from the embedded
 * engine. It is a local integration harness for demos, notebooks, containers,
 * and client smoke tests, not a production exchange/OMS adapter.</p>
 */
public final class SimulatorServerApplication {
    private SimulatorServerApplication() {
    }

    public static void main(final String[] args) throws Exception {
        final Options options = Options.parse(args);
        try (ServerRuntime runtime = start(options)) {
            Thread.currentThread().join();
        }
    }

    public static ServerRuntime start(final Options options) throws Exception {
        final ManualClock clock = new ManualClock(System.currentTimeMillis() * 1_000_000L);
        final SorObservability observability = SorObservability.create();
        final SorEngine engine = SorEngineBuilder.create()
                .config(new SorConfig(1024, 5_000))
                .marketData(new SimulatedMarketDataSource())
                .venueAdapter(new SimulatedVenueAdapter(clock))
                .riskProvider(new SimulatedRiskProvider())
                .persistence(new InMemoryPersistence())
                .clock(clock)
                .observability(observability)
                .build();
        engine.warmup(options.warmupOrders);
        final HttpControlPlaneServer http = new HttpControlPlaneServer(options.httpControlPort, engine, observability);
        http.start();
        final AeronSorServer aeron = "embedded-only".equals(options.transport)
                ? null
                : new AeronSorServer(options.aeronChannel, engine);
        if (aeron != null) {
            aeron.start();
        }
        final ServerRuntime runtime = new ServerRuntime(engine, http, aeron, options);
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close, "sor-test-server-shutdown"));
        return runtime;
    }

    public record Options(
            Path config,
            String transport,
            String aeronChannel,
            int httpControlPort,
            int warmupOrders,
            String heapSize,
            int metricsPort
    ) {
        public static Options parse(final String[] args) {
            Path config = null;
            String transport = "embedded-only";
            String aeronChannel = "aeron:ipc";
            int httpControlPort = 9090;
            int warmupOrders = 0;
            String heapSize = "8g";
            int metricsPort = 9090;
            for (String arg : args == null ? new String[0] : args) {
                if (arg.startsWith("--config=")) {
                    config = Path.of(value(arg));
                } else if (arg.startsWith("--transport=")) {
                    transport = value(arg).toLowerCase(Locale.ROOT);
                } else if (arg.startsWith("--aeron-channel=")) {
                    aeronChannel = value(arg);
                } else if (arg.startsWith("--http-control-port=")) {
                    httpControlPort = Integer.parseInt(value(arg));
                } else if (arg.startsWith("--warmup-orders=")) {
                    warmupOrders = Integer.parseInt(value(arg));
                } else if (arg.startsWith("--heap-size=")) {
                    heapSize = value(arg);
                } else if (arg.startsWith("--metrics-port=")) {
                    metricsPort = Integer.parseInt(value(arg));
                } else {
                    throw new IllegalArgumentException("unknown server flag: " + arg);
                }
            }
            if (!"embedded-only".equals(transport) && !"aeron".equals(transport)) {
                throw new IllegalArgumentException("--transport must be embedded-only or aeron");
            }
            return new Options(config, transport, aeronChannel, httpControlPort, warmupOrders, heapSize, metricsPort);
        }

        private static String value(final String arg) {
            return arg.substring(arg.indexOf('=') + 1);
        }
    }

    public static final class ServerRuntime implements AutoCloseable {
        private final SorEngine engine;
        private final HttpControlPlaneServer http;
        private final AeronSorServer aeron;
        private final Options options;

        ServerRuntime(final SorEngine engine, final HttpControlPlaneServer http,
                      final AeronSorServer aeron, final Options options) {
            this.engine = engine;
            this.http = http;
            this.aeron = aeron;
            this.options = options;
        }

        public int httpPort() {
            return http.port();
        }

        public Options options() {
            return options;
        }

        @Override public void close() {
            http.stop();
            if (aeron != null) {
                aeron.close();
            }
            engine.close();
        }
    }
}
