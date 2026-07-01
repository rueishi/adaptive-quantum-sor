package com.nitroj.sor.core.recovery;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.core.execution.PolicyDrivenSorExecutioner;
import com.nitroj.sor.core.execution.RouteDecisionResult;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;
import com.nitroj.sor.core.model.ChildOrderBuffer;
import com.nitroj.sor.core.model.OrderIntent;
import com.nitroj.sor.core.model.OrderStatus;
import com.nitroj.sor.core.model.Side;
import com.nitroj.sor.core.model.VenueStatus;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.publication.PublicationGate;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.state.VenueSessionState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify restart recovery safe-state behavior.
 *
 * <p>Role in system: covers P1-TC-026 by exercising snapshot restore,
 * baseline policy regeneration, invalid snapshot rejection, lifecycle emission,
 * and no-routing-before-policy-active behavior.</p>
 *
 * <p>Relationships: uses production recovery, publisher, lifecycle, execution,
 * market, session, and risk classes to prove the startup boundary integrates
 * with routing safety.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct task-card coverage.</p>
 *
 * <p>Design intent: keep recovery tests deterministic and small while proving
 * every public recovery contract.</p>
 */
final class StartupRecoveryCoordinatorTest {
    @Test
    void restartCreatesActivePolicyFromValidSnapshot() {
        final SorConfig config = config(1, 2, 1, 1);
        final SorPolicy snapshot = new InitialPolicyBootstrap().bootstrap(config);
        final InMemoryLifecycleEventStore events = new InMemoryLifecycleEventStore(8, true);
        final StartupRecoveryCoordinator coordinator = coordinator(events);

        final StartupRecoveryCoordinator.StartupRecoveryResult result =
                coordinator.recover(config, Optional.of(snapshot), 1L);

        assertTrue(result.safeToRoute());
        assertTrue(result.restoredSnapshot());
        assertFalse(result.bootstrappedBaseline());
        assertEquals("snapshot_restored", result.reason());
        assertEquals(snapshot, result.activePolicy());
        assertEquals(snapshot, coordinator.activePolicy());
        assertTrue(coordinator.safeToRoute());
        assertEquals(LifecycleEventType.POLICY_PUBLISHED, events.snapshot().getFirst().eventType);
    }

    @Test
    void missingSnapshotFallsBackToBootstrapPolicy() {
        final SorConfig config = config(1, 3, 2, 1);
        final InMemoryLifecycleEventStore events = new InMemoryLifecycleEventStore(8, true);
        final StartupRecoveryCoordinator coordinator = coordinator(events);

        final StartupRecoveryCoordinator.StartupRecoveryResult result =
                coordinator.recover(config, Optional.empty(), 2L);

        assertTrue(result.safeToRoute());
        assertFalse(result.restoredSnapshot());
        assertTrue(result.bootstrappedBaseline());
        assertEquals("baseline_bootstrapped", result.reason());
        assertNotNull(result.activePolicy());
        assertEquals(1, result.activePolicy().fullPolicyMatrix.instrumentCount);
        assertEquals(3, result.activePolicy().fullPolicyMatrix.venueCount);
        assertEquals(2, result.activePolicy().fullPolicyMatrix.regimeCount);
        assertEquals(1, result.activePolicy().fullPolicyMatrix.urgencyCount);
        assertEquals(LifecycleEventType.POLICY_PUBLISHED, events.snapshot().getFirst().eventType);
    }

    @Test
    void badSnapshotRejectedAndBaselinePublishedInstead() {
        final SorConfig snapshotConfig = config(1, 1, 1, 1);
        final SorConfig startupConfig = config(1, 2, 1, 1);
        final SorPolicy badSnapshot = new InitialPolicyBootstrap().bootstrap(snapshotConfig);
        final InMemoryLifecycleEventStore events = new InMemoryLifecycleEventStore(8, true);
        final StartupRecoveryCoordinator coordinator = coordinator(events);

        final StartupRecoveryCoordinator.StartupRecoveryResult result =
                coordinator.recover(startupConfig, Optional.of(badSnapshot), 3L);

        assertTrue(result.safeToRoute());
        assertFalse(result.restoredSnapshot());
        assertTrue(result.bootstrappedBaseline());
        assertEquals("baseline_bootstrapped", result.reason());
        assertEquals(2, result.activePolicy().fullPolicyMatrix.venueCount);
        assertEquals(LifecycleEventType.POLICY_REJECTED, events.snapshot().get(0).eventType);
        assertEquals(LifecycleEventType.POLICY_PUBLISHED, events.snapshot().get(1).eventType);
    }

    @Test
    void noRoutingBeforePolicyActiveAndRoutingAllowedAfterRecovery() {
        final SorConfig config = config(1, 1, 1, 1);
        final InMemoryPolicySnapshotStore store = new InMemoryPolicySnapshotStore();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), store);
        final MarketBookState market = new MarketBookState(1, 1);
        market.updateTopOfBook(0, 0, 100L, 101L, 1_000L, 1_000L);
        final VenueSessionState sessions = new VenueSessionState(1);
        sessions.setStatus(0, VenueStatus.OPEN);
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(1, 1);
        risk.setMaxChildQty(0, 500L);
        risk.setVenueLimits(0, 0, 1_000_000L, 2_500);
        final PolicyDrivenSorExecutioner executioner = new PolicyDrivenSorExecutioner(publisher, market, sessions, risk);

        final RouteDecisionResult before = executioner.route(order(100L), new ChildOrderBuffer(2));
        assertEquals(OrderStatus.NO_ACTIVE_POLICY, before.status);
        assertEquals(0, before.childOrderCount);
        assertEquals(100L, before.residualQty);

        final StartupRecoveryCoordinator coordinator = new StartupRecoveryCoordinator(
                publisher, new InitialPolicyBootstrap(), new InMemoryLifecycleEventStore(8, true));
        final StartupRecoveryCoordinator.StartupRecoveryResult recovery =
                coordinator.recover(config, Optional.empty(), 4L);
        final ChildOrderBuffer output = new ChildOrderBuffer(2);
        final RouteDecisionResult after = executioner.route(order(100L), output);

        assertTrue(recovery.safeToRoute());
        assertEquals(OrderStatus.ACKED, after.status);
        assertEquals(1, output.size());
        assertEquals(100L, after.routedQty);
    }

    @Test
    void invalidInputsAndBootstrapFailureLeaveSafeStateInactive() {
        final InMemoryLifecycleEventStore events = new InMemoryLifecycleEventStore(8, true);
        final StartupRecoveryCoordinator coordinator = coordinator(events);

        assertThrows(IllegalArgumentException.class, () -> new InitialPolicyBootstrap().bootstrap(null));
        assertThrows(IllegalArgumentException.class, () -> coordinator.recover(null, Optional.empty(), 1L));
        assertThrows(IllegalArgumentException.class, () -> coordinator.recover(config(1, 1, 1, 1), null, 1L));
        assertThrows(IllegalArgumentException.class, () -> coordinator.recover(config(1, 1, 1, 1), Optional.empty(), -1L));
        assertThrows(IllegalArgumentException.class, () -> new StartupRecoveryCoordinator(null,
                new InitialPolicyBootstrap(), events));
        assertThrows(IllegalArgumentException.class,
                () -> new StartupRecoveryCoordinator.StartupRecoveryResult(false, false, false, " ", null));

        final StartupRecoveryCoordinator failing = new StartupRecoveryCoordinator(
                new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore()),
                new InitialPolicyBootstrap() {
                    @Override
                    public SorPolicy bootstrap(final SorConfig config) {
                        throw new IllegalStateException("cannot compile baseline");
                    }
                },
                events
        );
        final StartupRecoveryCoordinator.StartupRecoveryResult result =
                failing.recover(config(1, 1, 1, 1), Optional.empty(), 5L);

        assertFalse(result.safeToRoute());
        assertEquals("bootstrap_failed", result.reason());
        assertNull(result.activePolicy());
        assertFalse(failing.safeToRoute());
    }

    private static StartupRecoveryCoordinator coordinator(final InMemoryLifecycleEventStore events) {
        return new StartupRecoveryCoordinator(
                new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore()),
                new InitialPolicyBootstrap(),
                events
        );
    }

    private static SorConfig config(final int instruments, final int venues, final int regimes, final int urgencies) {
        return new SorConfig(instruments, venues, regimes, urgencies, SorConfig.RuntimeMode.DEMO, true);
    }

    private static OrderIntent order(final long quantity) {
        return new OrderIntent(77L, 0, Side.BUY, quantity, 0, 1L);
    }
}
