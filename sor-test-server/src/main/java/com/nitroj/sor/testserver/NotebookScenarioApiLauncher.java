package com.nitroj.sor.testserver;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.core.execution.PolicyDrivenSorExecutioner;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;
import com.nitroj.sor.core.metadata.InstrumentMetadata;
import com.nitroj.sor.core.metadata.VenueMetadata;
import com.nitroj.sor.core.model.ChildOrderBuffer;
import com.nitroj.sor.core.model.ParentOrderIntentQueue;
import com.nitroj.sor.core.model.VenueStatus;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.publication.PublicationGate;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;
import com.nitroj.sor.core.recovery.InitialPolicyBootstrap;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.state.VenueSessionState;

import java.util.List;

/**
 * Responsibility: start the notebook/demo scenario HTTP API as a standalone
 * local process.
 *
 * <p>Role in system: provides a tiny launch entrypoint for Jupyter notebooks
 * and manual scenario experimentation when the full simulator sample server is
 * not needed. It creates a permissive baseline policy, a deterministic market
 * book, open venue sessions, and risk limits, then wires those into
 * {@link NotebookScenarioHttpServer}.</p>
 *
 * <p>Relationships: uses core policy, execution, market-state, metadata, and
 * lifecycle classes directly because this launcher is a test-server assembly
 * point. Reusable scenario logic remains in {@code sor-testkit}; user-facing
 * production HTTP control remains in {@code sor-transport-http-control}.</p>
 *
 * <p>Lifecycle: invoked from the {@code runNotebookApi} Gradle task or from a
 * command line. It starts one HTTP server on localhost, installs a shutdown
 * hook, prints the selected port, and then parks the main thread until the
 * process is stopped.</p>
 *
 * <p>Design intent: make notebook demos reproducible with no external OMS,
 * EMS, venue, market-data, or policy-bootstrap service. The baseline data is
 * intentionally synthetic and belongs to the test server only.</p>
 */
public final class NotebookScenarioApiLauncher {
    private NotebookScenarioApiLauncher() {
    }

    public static void main(final String[] args) throws Exception {
        final int port = parsePort(args);
        final SorConfig config = new SorConfig(20, 100, 5, 4, SorConfig.RuntimeMode.DEMO, true);
        final InMemoryLifecycleEventStore lifecycle = new InMemoryLifecycleEventStore(512, true);
        final PolicyPublisher publisher = new PolicyPublisher(
                PublicationGate.permissive(),
                new InMemoryPolicySnapshotStore());
        publisher.publish(
                new InitialPolicyBootstrap().bootstrap(config),
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                new PolicyDiff(),
                1,
                1L);
        lifecycle.append(new LifecycleEvent(1L, 1L, 1, LifecycleEventType.METRICS_SUMMARY, 1L,
                "notebook scenario api ready"));

        final MarketBookState market = baselineMarket(config);
        final NotebookScenarioHttpServer server = new NotebookScenarioHttpServer(
                port,
                new ParentOrderIntentQueue(1024),
                publisher,
                lifecycle,
                config,
                new PolicyDrivenSorExecutioner(publisher, market, baselineSessions(config), baselineRisk(config)),
                new ChildOrderBuffer(Math.max(1, Math.min(16, config.venueCount()))),
                market,
                InstrumentMetadata.simulated(config.instrumentCount()),
                VenueMetadata.simulated(config.instrumentCount(), config.venueCount()));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "notebook-scenario-api-shutdown"));
        System.out.println("Notebook scenario API on http://127.0.0.1:" + server.port());
        Thread.currentThread().join();
    }

    private static int parsePort(final String[] args) {
        int port = 8080;
        for (String arg : args == null ? new String[0] : args) {
            if (arg.startsWith("--http-control-port=") || arg.startsWith("--api-port=")) {
                port = Integer.parseInt(arg.substring(arg.indexOf('=') + 1));
            } else {
                throw new IllegalArgumentException("unknown notebook api flag: " + arg);
            }
        }
        return port;
    }

    private static MarketBookState baselineMarket(final SorConfig config) {
        final MarketBookState market = new MarketBookState(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final long mid = 10_000L + instrumentId * 10L + venueId;
                final long displayedQty = 5_000L + venueId * 100L;
                market.updateTopOfBook(instrumentId, venueId, mid - 1L, mid + 1L, displayedQty, displayedQty);
            }
        }
        return market;
    }

    private static VenueSessionState baselineSessions(final SorConfig config) {
        final VenueSessionState sessions = new VenueSessionState(config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            sessions.setStatus(venueId, VenueStatus.OPEN);
        }
        return sessions;
    }

    private static RiskLimitSnapshot baselineRisk(final SorConfig config) {
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(config.instrumentCount(), config.venueCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            risk.setMaxChildQty(instrumentId, 1_000L);
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                risk.setVenueLimits(instrumentId, venueId, 10_000_000L, 2_500);
            }
        }
        return risk;
    }
}
