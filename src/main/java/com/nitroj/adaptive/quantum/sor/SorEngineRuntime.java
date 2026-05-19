package com.nitroj.adaptive.quantum.sor;

import com.nitroj.adaptive.quantum.sor.api.SorHttpApiServer;
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
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.recovery.InitialPolicyBootstrap;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Responsibility: own the long-running in-process SOR engine state.
 *
 * <p>Role in system: notebooks and Python tools should mutate this engine
 * through the HTTP control plane rather than starting their own parallel demo
 * state. The runtime owns the parent-order queue, active policy publisher,
 * lifecycle events, and optional API server.</p>
 */
public final class SorEngineRuntime implements AutoCloseable {
    private final SorConfig config;
    private final ParentOrderIntentQueue orderQueue;
    private final PolicyPublisher publisher;
    private final InMemoryLifecycleEventStore lifecycleStore;
    private final SorHttpApiServer apiServer;
    private final CountDownLatch shutdown = new CountDownLatch(1);

    private SorEngineRuntime(
            final SorConfig config,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore,
            final SorHttpApiServer apiServer
    ) {
        this.config = config;
        this.orderQueue = orderQueue;
        this.publisher = publisher;
        this.lifecycleStore = lifecycleStore;
        this.apiServer = apiServer;
    }

    public static SorEngineRuntime create(final SorConfig config, final int apiPort) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(1024);
        final InMemoryLifecycleEventStore lifecycle = new InMemoryLifecycleEventStore(256, true);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        final MarketBookState market = baselineMarket(config);
        final VenueSessionState sessions = baselineSessions(config);
        final RiskLimitSnapshot risk = baselineRisk(config);
        final InstrumentMetadata instruments = InstrumentMetadata.simulated(config.instrumentCount());
        final VenueMetadata venues = VenueMetadata.simulated(config.instrumentCount(), config.venueCount());
        final PolicyDrivenSorExecutioner executioner = new PolicyDrivenSorExecutioner(publisher, market, sessions, risk);
        final ChildOrderBuffer childOrderBuffer = new ChildOrderBuffer(Math.max(1, Math.min(16, config.venueCount())));
        publisher.publish(
                new InitialPolicyBootstrap().bootstrap(config),
                PolicyValidationReport.validReport(),
                new com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport(java.util.List.of()),
                new PolicyDiff(),
                1,
                1L
        );
        lifecycle.append(new LifecycleEvent(1L, 1L, 1, LifecycleEventType.METRICS_SUMMARY, 1L,
                "engine runtime ready"));
        return new SorEngineRuntime(
                config,
                queue,
                publisher,
                lifecycle,
                new SorHttpApiServer(apiPort, queue, publisher, lifecycle, config, executioner, childOrderBuffer,
                        market, instruments, venues)
        );
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

    public void start() {
        apiServer.start();
    }

    public void awaitShutdown() throws InterruptedException {
        shutdown.await();
    }

    public int apiPort() {
        return apiServer.port();
    }

    public SorHttpApiServer apiServer() {
        return apiServer;
    }

    public SorConfig config() {
        return config;
    }

    public ParentOrderIntentQueue orderQueue() {
        return orderQueue;
    }

    public PolicyPublisher publisher() {
        return publisher;
    }

    public InMemoryLifecycleEventStore lifecycleStore() {
        return lifecycleStore;
    }

    @Override
    public void close() {
        apiServer.stop();
        shutdown.countDown();
    }
}
