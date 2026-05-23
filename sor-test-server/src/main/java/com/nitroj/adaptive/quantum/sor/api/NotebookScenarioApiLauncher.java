package com.nitroj.adaptive.quantum.sor.api;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;
import com.nitroj.adaptive.quantum.sor.metadata.InstrumentMetadata;
import com.nitroj.adaptive.quantum.sor.metadata.VenueMetadata;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.recovery.InitialPolicyBootstrap;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

import java.util.List;

/** Starts the legacy notebook scenario API used by the Jupyter demo notebooks. */
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
        final SorHttpApiServer server = new SorHttpApiServer(
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
