package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.execution.ExecutionFixtures;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.RouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.metadata.InstrumentMetadata;
import com.nitroj.adaptive.quantum.sor.metadata.VenueMetadata;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic simulated external source generators.
 *
 * <p>Role in system: proves P1-TC-007 simulators emit valid bounded state and
 * repeatable seed-driven sequences.</p>
 *
 * <p>Relationships: covers simulator writes into model, market, venue, fee,
 * risk, metadata, and scenario state structures.</p>
 *
 * <p>Lifecycle: executed as unit coverage for simulator task-card behavior.</p>
 *
 * <p>Design intent: focus on simulator contracts instead of exchange matching
 * fidelity, which is explicitly out of scope.</p>
 */
final class SimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void parentOrderSimulatorProducesRepeatableBoundedSequence() {
        final ParentOrderIntentSimulator first = new ParentOrderIntentSimulator(CONFIG, 99L);
        final ParentOrderIntentSimulator second = new ParentOrderIntentSimulator(CONFIG, 99L);

        for (int i = 0; i < 25; i++) {
            final OrderIntent a = first.next(100L + i);
            final OrderIntent b = second.next(100L + i);

            assertEquals(i + 1L, a.parentOrderId);
            assertEquals(a.parentOrderId, b.parentOrderId);
            assertEquals(a.instrumentId, b.instrumentId);
            assertEquals(a.side, b.side);
            assertEquals(a.quantity, b.quantity);
            assertEquals(a.urgencyId, b.urgencyId);
            assertEquals(100L + i, a.createdAtNanos);
            assertTrue(a.instrumentId >= 0 && a.instrumentId < CONFIG.instrumentCount());
            assertTrue(Side.isValid(a.side));
            assertTrue(a.quantity >= 100L && a.quantity <= 10_000L);
            assertTrue(a.urgencyId >= 0 && a.urgencyId < CONFIG.urgencyCount());
        }
    }

    @Test
    void marketDataSimulatorPopulatesEveryBookCellWithRepeatableValidValues() {
        final MarketBookState firstState = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketBookState secondState = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator first = new MarketDataSimulator(CONFIG, 7L);
        final MarketDataSimulator second = new MarketDataSimulator(CONFIG, 7L);

        first.generateTick(firstState);
        second.generateTick(secondState);

        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertTrue(firstState.bidPriceTicks(instrumentId, venueId) > 0);
                assertTrue(firstState.askPriceTicks(instrumentId, venueId) > firstState.bidPriceTicks(instrumentId, venueId));
                assertTrue(firstState.bidQty(instrumentId, venueId) >= 0);
                assertTrue(firstState.askQty(instrumentId, venueId) >= 0);
                assertEquals(firstState.bidPriceTicks(instrumentId, venueId), secondState.bidPriceTicks(instrumentId, venueId));
                assertEquals(firstState.askPriceTicks(instrumentId, venueId), secondState.askPriceTicks(instrumentId, venueId));
                assertEquals(firstState.bidQty(instrumentId, venueId), secondState.bidQty(instrumentId, venueId));
                assertEquals(firstState.askQty(instrumentId, venueId), secondState.askQty(instrumentId, venueId));
            }
        }
    }

    @Test
    void marketDataSimulatorSanitizesInvalidExternalValues() {
        final MarketBookState state = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, 7L);

        simulator.applySanitized(state, 0, 0, -1, -2, -3, -4);
        assertEquals(1L, state.bidPriceTicks(0, 0));
        assertEquals(2L, state.askPriceTicks(0, 0));
        assertEquals(0L, state.bidQty(0, 0));
        assertEquals(0L, state.askQty(0, 0));

        simulator.applySanitized(state, 0, 1, 100L, 100L, 50L, 60L);
        assertEquals(100L, state.bidPriceTicks(0, 1));
        assertEquals(101L, state.askPriceTicks(0, 1));
        assertEquals(50L, state.bidQty(0, 1));
        assertEquals(60L, state.askQty(0, 1));
    }

    @Test
    void venueBehaviorSimulatorInitializesRealisticProfiles() {
        final VenueBehaviorSimulator simulator = new VenueBehaviorSimulator(CONFIG, 4L);

        assertEquals(100, simulator.behaviorState().toxicityBps(0));
        assertEquals(200, simulator.behaviorState().toxicityBps(1));
        assertEquals(2_500, simulator.behaviorState().toxicityBps(2));
        assertEquals(300, simulator.behaviorState().toxicityBps(3));
        assertEquals(800, simulator.behaviorState().toxicityBps(4));
    }

    @Test
    void venueBehaviorSimulatorGeneratesAckAndTerminalOutcomeForEveryChildOrder() {
        final ChildOrderBuffer buffer = new ChildOrderBuffer(3);
        buffer.add(1L, 10L, 0, 0, Side.BUY, 1_000L, 1L, 2L);
        buffer.add(2L, 10L, 0, 1, Side.SELL, 1_000L, 1L, 2L);
        buffer.add(3L, 10L, 0, 4, Side.BUY, 1_000L, 1L, 2L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(10);
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(3);
        final ChildOrderState childState = new ChildOrderState(3);

        final VenueBehaviorSimulator simulator = new VenueBehaviorSimulator(CONFIG, 4L);
        simulator.process(buffer, outcomes, outstanding, childState);

        assertEquals(6, outcomes.size());
        assertEquals(1L, outcomes.childOrderId(0));
        assertEquals(ExecutionOutcomeStore.ACK, outcomes.outcomeType(0));
        assertTrue(ExecutionOutcomeStore.isFill(outcomes.outcomeType(1)) || ExecutionOutcomeStore.isReject(outcomes.outcomeType(1)));
        assertEquals(1L, outstanding.childOrderId(0));
        assertEquals(2L, outstanding.childOrderId(1));
        assertEquals(3L, outstanding.childOrderId(2));
        assertTrue(outstanding.remainingQty(0) >= 0);
        assertTrue(childState.status(0) == OrderStatus.FILLED
                || childState.status(0) == OrderStatus.PARTIALLY_FILLED
                || childState.status(0) == OrderStatus.REJECTED);
        assertTrue(outcomes.latencyNanos(0) > 0);
        assertTrue(outcomes.slippageBps(1) >= 0 && outcomes.slippageBps(1) <= 49);
        assertTrue(simulator.behaviorState().toxicityBps(2) > simulator.behaviorState().toxicityBps(0));
    }

    @Test
    void sessionThrottleFeeRiskMetadataAndScenarioGeneratorsPopulateAllOwnedState() {
        final VenueSessionState venueSession = new VenueSessionState(CONFIG.venueCount());
        new VenueSessionSimulator(CONFIG).applyOutagePattern(venueSession);
        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            final int expected = venueId % 5 == 4 ? VenueStatus.OUTAGE : VenueStatus.OPEN;
            assertEquals(expected, venueSession.status(venueId));
        }
        assertFalse(venueSession.isAvailable(4));

        final VenueThrottleState throttle = new VenueThrottleState(CONFIG.venueCount());
        new VenueThrottleSimulator(CONFIG).populate(throttle);
        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            assertEquals(50 + venueId * 5, throttle.maxOrderRatePerSecond(venueId));
        }

        final MarketSessionState marketSession = new MarketSessionState(CONFIG.instrumentCount());
        new MarketSessionSimulator(CONFIG).openAll(marketSession);
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(marketSession.isOpen(instrumentId));
        }

        final FeeScheduleSnapshot fees = new FeeScheduleSimulator(CONFIG).generate();
        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            assertEquals(venueId % 3, fees.makerFeeTicks(0, venueId));
            assertEquals(1 + venueId % 5, fees.takerFeeTicks(0, venueId));
        }
        final RiskLimitSnapshot risk = new RiskLimitSimulator(CONFIG).generate();
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertEquals(10_000L + instrumentId * 100L, risk.maxChildQty(instrumentId));
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertEquals(1_000_000L + venueId * 10_000L, risk.maxVenueNotional(instrumentId, venueId));
                assertEquals(2_500, risk.maxParticipationBps(instrumentId, venueId));
            }
        }
        final InstrumentMetadata instruments = new InstrumentMetadataSimulator(CONFIG).generate();
        assertEquals(CONFIG.instrumentCount(), instruments.instrumentCount());
        assertEquals("INST1", instruments.symbol(1));
        assertTrue(instruments.isEnabled(1));
        final VenueMetadata venues = new VenueMetadataSimulator(CONFIG).generate();
        assertEquals(CONFIG.venueCount(), venues.venueCount());
        assertEquals("VENUE4", venues.venueName(4));
        assertTrue(venues.isEnabled(4));
        assertTrue(venues.supportsInstrument(1, 4));

        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        new SyntheticScenarioGenerator(CONFIG, 3L).populateBaseline(book, venueSession, throttle, marketSession);
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            assertTrue(marketSession.isOpen(instrumentId));
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertTrue(book.askPriceTicks(instrumentId, venueId) > book.bidPriceTicks(instrumentId, venueId));
                assertTrue(venueSession.isAvailable(venueId));
                assertEquals(50 + venueId * 5, throttle.maxOrderRatePerSecond(venueId));
            }
        }
    }

    @Test
    void zeroLiquidityVenueScenarioProducesNoChildOrdersAndResidualQuantity() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        fixture.market().updateTopOfBook(0, 0, 100L, 101L, 0L, 0L);
        fixture.market().updateTopOfBook(0, 1, 100L, 102L, 0L, 0L);
        fixture.market().updateTopOfBook(0, 2, 100L, 103L, 0L, 0L);

        final RouteDecisionResult result = new PolicyDrivenSorExecutioner(
                fixture.publisher(),
                fixture.market(),
                fixture.sessions(),
                fixture.risk()
        ).route(ExecutionFixtures.buy(250L), new ChildOrderBuffer(4));

        assertEquals(OrderStatus.NO_LIQUIDITY, result.status);
        assertEquals(0, result.childOrderCount);
        assertEquals(250L, result.residualQty);
    }

    @Test
    void venueOutageScenarioIsSkippedByExecutioner() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        fixture.sessions().setStatus(0, VenueStatus.OUTAGE);
        fixture.market().updateTopOfBook(0, 0, 100L, 101L, 1_000L, 1_000L);
        fixture.market().updateTopOfBook(0, 1, 100L, 102L, 1_000L, 1_000L);
        fixture.market().updateTopOfBook(0, 2, 100L, 103L, 1_000L, 1_000L);
        assertFalse(fixture.sessions().isAvailable(0));

        final ChildOrderBuffer output = new ChildOrderBuffer(4);
        final RouteDecisionResult result = new PolicyDrivenSorExecutioner(
                fixture.publisher(),
                fixture.market(),
                fixture.sessions(),
                fixture.risk()
        ).route(ExecutionFixtures.buy(600L), output);

        assertEquals(OrderStatus.ACKED, result.status);
        for (int i = 0; i < output.size(); i++) {
            assertTrue(fixture.sessions().isAvailable(output.get(i).venueId));
        }
    }

    @Test
    void simulatorSupervisorLeavesHealthyRunPublishable() {
        final SimulatorHealthState health = new SimulatorHealthState();
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(4, true);
        final SimulatorSupervisor supervisor = new SimulatorSupervisor(health, lifecycleEvents);

        final boolean completed = supervisor.runGuarded("MarketDataSimulator", 12L, () -> {
        });

        assertTrue(completed);
        assertTrue(health.healthy());
        assertEquals(0, health.failureCount());
        assertTrue(new SimulatorPublicationGuard(health).evaluate().publishAllowed);
        assertTrue(lifecycleEvents.snapshot().isEmpty());
    }

    @Test
    void simulatorSupervisorRecordsFailureHealthLifecycleAndPublicationGuard() {
        final SimulatorHealthState health = new SimulatorHealthState();
        final InMemoryLifecycleEventStore lifecycleEvents = new InMemoryLifecycleEventStore(4, true);
        final SimulatorSupervisor supervisor = new SimulatorSupervisor(health, lifecycleEvents);

        final boolean completed = supervisor.runGuarded(
                "VenueBehaviorSimulator",
                42L,
                () -> {
                    throw new IllegalStateException("venue loop exploded");
                }
        );
        final List<LifecycleEvent> events = lifecycleEvents.snapshot();

        assertFalse(completed);
        assertFalse(health.healthy());
        assertEquals(1, health.failureCount());
        assertEquals("VenueBehaviorSimulator", health.failedSimulatorName());
        assertEquals("venue loop exploded", health.failureMessage());
        assertEquals(42L, health.failedAtNanos());
        assertEquals(1, events.size());
        assertEquals(LifecycleEventType.SIMULATOR_FAILURE, events.getFirst().eventType);
        assertTrue(events.getFirst().message.contains("VenueBehaviorSimulator"));
        assertTrue(events.getFirst().message.contains("venue loop exploded"));
        assertFalse(new SimulatorPublicationGuard(health).evaluate().publishAllowed);
        assertEquals("simulator_failed", new SimulatorPublicationGuard(health).evaluate().failedGates[0]);
    }

    @Test
    void simulatorHealthRecoveryAllowsPublicationGuardAgain() {
        final SimulatorHealthState health = new SimulatorHealthState();
        health.markFailed("MarketDataSimulator", "bad tick", 7L);

        assertFalse(new SimulatorPublicationGuard(health).evaluate().publishAllowed);
        health.markHealthy();

        assertTrue(health.healthy());
        assertEquals("", health.failedSimulatorName());
        assertEquals("", health.failureMessage());
        assertEquals(-1L, health.failedAtNanos());
        assertEquals(1, health.failureCount(), "failure count remains observable after recovery");
        assertTrue(new SimulatorPublicationGuard(health).evaluate().publishAllowed);
    }

    @Test
    void simulatorsRejectInvalidConstructionInputs() {
        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new MarketDataSimulator(null, 1L)
        ).getMessage());
        assertEquals("venue behavior inputs must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new VenueBehaviorSimulator(CONFIG, 1L).process(null, null, null, null)
        ).getMessage());
        assertEquals("state must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new MarketDataSimulator(CONFIG, 1L).generateTick(null)
        ).getMessage());
        assertThrows(IllegalArgumentException.class, () -> new ParentOrderIntentSimulator(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> new VenueSessionSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new VenueThrottleSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new MarketSessionSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new FeeScheduleSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new RiskLimitSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new InstrumentMetadataSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new VenueMetadataSimulator(null));
        assertThrows(IllegalArgumentException.class, () -> new SyntheticScenarioGenerator(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorSupervisor(null, new InMemoryLifecycleEventStore(1, true)));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorSupervisor(new SimulatorHealthState(), null));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorPublicationGuard(null));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorHealthState().markFailed("", "bad", 1L));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorHealthState().markFailed("sim", "", 1L));
        assertThrows(IllegalArgumentException.class, () -> new SimulatorHealthState().markFailed("sim", "bad", -1L));
        final SimulatorSupervisor supervisor = new SimulatorSupervisor(
                new SimulatorHealthState(),
                new InMemoryLifecycleEventStore(1, true)
        );
        assertThrows(IllegalArgumentException.class, () -> supervisor.runGuarded("", 1L, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> supervisor.runGuarded("sim", -1L, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> supervisor.runGuarded("sim", 1L, null));
    }
}
