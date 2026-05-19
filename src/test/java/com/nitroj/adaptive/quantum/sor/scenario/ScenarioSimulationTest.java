package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.sim.MarketDataSimulator;
import com.nitroj.adaptive.quantum.sor.sim.MarketSessionSimulator;
import com.nitroj.adaptive.quantum.sor.sim.ParentOrderIntentSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueBehaviorSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueSessionSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueThrottleSimulator;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.state.FeedHealthState;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify the initial scenario runner contract.
 *
 * <p>Role in system: establishes the P5-TC-001 Gradle/JUnit entry point for
 * scenario-driven tests before later task cards wire in full market and routing
 * behavior.</p>
 *
 * <p>Relationships: tests {@link ScenarioRunner}, {@link ScenarioState}, and
 * {@link ScenarioSpec} together.</p>
 *
 * <p>Lifecycle: executed separately from deterministic simulator contract
 * tests.</p>
 *
 * <p>Design intent: prove scenario summaries are deterministic and that
 * scenario package orchestration is distinct from low-level `sim` classes.</p>
 */
final class ScenarioSimulationTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void scenarioRunnerReturnsDeterministicSummaryForFixedSeed() {
        final ScenarioSpec spec = new ScenarioSpec("n-v-t", 17L, 6, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 1, 0),
                        new ScenarioWindow(2, 3, 1),
                        new ScenarioWindow(4, 5, 2)
                },
                true,
                ScenarioSimulatorConfig.defaults());

        final ScenarioSummary first = new ScenarioRunner().run(spec);
        final ScenarioSummary second = new ScenarioRunner().run(spec);

        ScenarioAssertions.assertReplayEquivalent(first, second);
        assertEquals(6, first.ticksRun());
        assertEquals(2, first.detectedNormalCount());
        assertEquals(2, first.detectedVolatileCount());
        assertEquals(2, first.detectedThinBookCount());
    }

    @Test
    void scenarioStateAssignsDeterministicVenueProfiles() {
        final ScenarioState state = ScenarioState.fresh(ScenarioSpec.defaultSpec(CONFIG, 5L));

        assertEquals(ScenarioVenueProfile.TIGHT_DEEP, state.profile(0));
        assertEquals(ScenarioVenueProfile.OUTAGE_PRONE, state.profile(4));
        assertThrows(IndexOutOfBoundsException.class, () -> state.profile(5));
        assertThrows(IllegalArgumentException.class, () -> ScenarioState.fresh(null));
    }

    @Test
    void sameSeedProducesIdenticalMultiTickBooks() {
        final ScenarioRun first = runBookScenario(17L, normalVolatileThinSpec(17L));
        final ScenarioRun second = runBookScenario(17L, normalVolatileThinSpec(17L));

        assertBookEquals(first.book, second.book);
        assertEquals(first.midChecksum, second.midChecksum);
        assertEquals(first.feedChecksum, second.feedChecksum);
        assertEquals(first.simulator.tick(), second.simulator.tick());
    }

    @Test
    void differentSeedChangesScenarioSummary() {
        final ScenarioSpec firstSpec = normalVolatileThinSpec(18L);
        final ScenarioSpec secondSpec = normalVolatileThinSpec(19L);

        final ScenarioSummary first = new ScenarioRunner().run(firstSpec);
        final ScenarioSummary second = new ScenarioRunner().run(secondSpec);

        assertNotEquals(first.bookChecksum(), second.bookChecksum());
    }

    @Test
    void normalWindowHasBoundedCorrelatedMidMovement() {
        final ScenarioSpec spec = new ScenarioSpec("normal-only", 23L, 12, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 11, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioRun run = runBookScenario(23L, spec);

        assertTrue(run.maxMidStep <= 1L);
        assertTrue(run.maxSameInstrumentVenueMidDistance <= 8L);
    }

    @Test
    void volatileWindowMovesMoreThanNormalWindow() {
        final long normalMovement = runBookScenario(29L, new ScenarioSpec("normal-only", 29L, 30, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 29, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults())).totalAbsMidMovement;
        final long volatileMovement = runBookScenario(29L, new ScenarioSpec("volatile-only", 29L, 30, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 29, RegimeState.VOLATILE)},
                true,
                ScenarioSimulatorConfig.defaults())).totalAbsMidMovement;

        assertTrue(volatileMovement > normalMovement);
    }

    @Test
    void thinBookWindowHasLowerMedianDisplayedQty() {
        final long normalQty = runBookScenario(31L, new ScenarioSpec("normal-only", 31L, 20, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 19, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults())).totalDisplayedQty;
        final long thinQty = runBookScenario(31L, new ScenarioSpec("thin-only", 31L, 20, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 19, RegimeState.THIN_BOOK)},
                true,
                ScenarioSimulatorConfig.defaults())).totalDisplayedQty;

        assertTrue(thinQty < normalQty);
    }

    @Test
    void venueQuotesRemainCorrelatedThroughSharedInstrumentMid() {
        final ScenarioRun run = runBookScenario(37L, normalVolatileThinSpec(37L));

        assertTrue(run.maxSameInstrumentVenueMidDistance <= 16L);
        assertTrue(run.maxSameVenueCrossInstrumentMidDistance >= 80L);
    }

    @Test
    void staleFeedProfilePreservesOrMarksQuoteDeterministically() {
        final ScenarioRun first = runBookScenario(43L, new ScenarioSpec("stale-observable", 43L, 40, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 39, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final ScenarioRun second = runBookScenario(43L, new ScenarioSpec("stale-observable", 43L, 40, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 39, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults()));

        assertTrue(first.staleEventCount > 0);
        assertEquals(first.staleEventCount, second.staleEventCount);
        assertEquals(first.feedChecksum, second.feedChecksum);
        assertBookEquals(first.book, second.book);
    }

    @Test
    void outageProneProfileProducesDeterministicOutageWindows() {
        final ScenarioSpec spec = new ScenarioSpec("venue-outage", 47L, 4, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 3, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioState firstScenario = ScenarioState.fresh(spec);
        final ScenarioState secondScenario = ScenarioState.fresh(spec);
        final VenueSessionState first = new VenueSessionState(CONFIG.venueCount());
        final VenueSessionState second = new VenueSessionState(CONFIG.venueCount());
        final VenueSessionSimulator simulator = new VenueSessionSimulator(CONFIG);

        simulator.applyScenario(first, firstScenario);
        simulator.applyScenario(second, secondScenario);
        assertEquals(VenueStatus.OUTAGE, first.status(4));
        assertEquals(first.status(4), second.status(4));
        firstScenario.clock().advance();
        firstScenario.clock().advance();
        secondScenario.clock().advance();
        secondScenario.clock().advance();
        simulator.applyScenario(first, firstScenario);
        simulator.applyScenario(second, secondScenario);
        assertEquals(VenueStatus.OPEN, first.status(4));
        assertEquals(first.status(4), second.status(4));
    }

    @Test
    void haltedOrAuctionInstrumentsAreReflectedInMarketSessionState() {
        final ScenarioState scenario = ScenarioState.fresh(new ScenarioSpec("auction-halt", 53L, 2, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, RegimeState.THIN_BOOK)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final MarketSessionState state = new MarketSessionState(CONFIG.instrumentCount());
        final MarketSessionSimulator simulator = new MarketSessionSimulator(CONFIG);

        simulator.applyScenario(state, scenario);
        assertFalse(state.isOpen(0));
        assertTrue(state.isOpen(1));
        scenario.clock().advance();
        simulator.applyScenario(state, scenario);
        assertTrue(state.isOpen(0));
        assertTrue(state.isOpen(1));
    }

    @Test
    void feedStalenessUpdatesFeedHealthWithoutInvalidBooks() {
        final ScenarioSpec spec = new ScenarioSpec("feed-stale", 59L, 50, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 49, RegimeState.NORMAL)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioRun run = runBookScenario(59L, spec);

        assertTrue(run.staleEventCount > 0);
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertTrue(run.book.askPriceTicks(instrumentId, venueId) > run.book.bidPriceTicks(instrumentId, venueId));
                assertTrue(run.book.bidQty(instrumentId, venueId) >= 0);
                assertTrue(run.book.askQty(instrumentId, venueId) >= 0);
            }
        }
    }

    @Test
    void parentOrderFlowChangesByRegimeUsingSimulatedClock() {
        final ParentOrderIntentSimulator simulator = new ParentOrderIntentSimulator(CONFIG, 61L);
        final ScenarioState volatileScenario = ScenarioState.fresh(new ScenarioSpec("volatile-flow", 61L, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, RegimeState.VOLATILE)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final ScenarioState thinScenario = ScenarioState.fresh(new ScenarioSpec("thin-flow", 61L, 2, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, RegimeState.THIN_BOOK)},
                true,
                ScenarioSimulatorConfig.defaults()));
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(4);

        assertEquals(2, simulator.offerForCurrentTick(volatileScenario, queue));
        final OrderIntent first = queue.poll();
        assertNotNull(first);
        assertEquals(0L, first.createdAtNanos);
        assertTrue(first.quantity >= 5_000L);
        assertEquals(1, simulator.offerForCurrentTick(thinScenario, queue));
        thinScenario.clock().advance();
        assertEquals(0, simulator.offerForCurrentTick(thinScenario, queue));
    }

    @Test
    void lowDisplayedLiquidityIncreasesPartialOrNoFillBehavior() {
        final VenueOutcomeRun run = runVenueOutcome(67L, RegimeState.NORMAL, 0, 100L, 1_000L, true);

        assertNotEquals(ExecutionOutcomeStore.FULL_FILL, run.outcomes.outcomeType(1));
        assertTrue(run.outcomes.filledQty(1) <= 100L);
        assertTrue(run.outstanding.remainingQty(0) >= 900L);
    }

    @Test
    void toxicProfileIncreasesSlippageRelativeToTightDeepProfile() {
        final VenueOutcomeRun tight = runVenueOutcome(71L, RegimeState.NORMAL, 0, 5_000L, 1_000L, true);
        final VenueOutcomeRun toxic = runVenueOutcome(71L, RegimeState.NORMAL, 2, 5_000L, 1_000L, true);

        assertTrue(toxic.outcomes.slippageBps(1) > tight.outcomes.slippageBps(1));
        assertTrue(toxic.outcomes.toxicityBps(1) > tight.outcomes.toxicityBps(1));
    }

    @Test
    void unavailableVenueRejectsOrSkipsAccordingToContract() {
        final VenueOutcomeRun run = runVenueOutcome(73L, RegimeState.NORMAL, 0, 5_000L, 1_000L, false);

        assertEquals(ExecutionOutcomeStore.ACK, run.outcomes.outcomeType(0));
        assertEquals(ExecutionOutcomeStore.REJECT, run.outcomes.outcomeType(1));
        assertEquals(0L, run.outcomes.filledQty(1));
    }

    @Test
    void stressedOrVolatileRegimeIncreasesLatencyRejectOrSlippage() {
        final VenueOutcomeRun normal = runVenueOutcome(79L, RegimeState.NORMAL, 0, 5_000L, 1_000L, true);
        final VenueOutcomeRun volatileRun = runVenueOutcome(79L, RegimeState.VOLATILE, 0, 5_000L, 1_000L, true);

        assertTrue(volatileRun.outcomes.latencyNanos(1) > normal.outcomes.latencyNanos(1));
        assertTrue(volatileRun.outcomes.slippageBps(1) >= normal.outcomes.slippageBps(1));
    }

    @Test
    void impossibleRawOutcomeQuantitiesCannotPublish() {
        final VenueOutcomeRun run = runVenueOutcome(83L, RegimeState.NORMAL, 0, 10L, 100L, true);

        assertTrue(run.outcomes.filledQty(1) <= 10L);
        assertTrue(run.outcomes.filledQty(1) <= 100L);
        assertTrue(run.outstanding.remainingQty(0) >= 90L);
    }

    private static ScenarioSpec normalVolatileThinSpec(final long seed) {
        return new ScenarioSpec("n-v-t", seed, 15, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 4, RegimeState.NORMAL),
                        new ScenarioWindow(5, 9, RegimeState.VOLATILE),
                        new ScenarioWindow(10, 14, RegimeState.THIN_BOOK)
                },
                true,
                ScenarioSimulatorConfig.defaults());
    }

    private static VenueOutcomeRun runVenueOutcome(
            final long seed,
            final int regimeId,
            final int venueId,
            final long visibleQty,
            final long orderQty,
            final boolean venueOpen
    ) {
        final ScenarioSpec spec = new ScenarioSpec("venue-outcome", seed, 1, CONFIG,
                new ScenarioWindow[]{new ScenarioWindow(0, 0, regimeId)},
                true,
                ScenarioSimulatorConfig.defaults());
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        for (int v = 0; v < CONFIG.venueCount(); v++) {
            book.updateTopOfBook(0, v, 100L, 101L + v, visibleQty, visibleQty);
            book.updateTopOfBook(1, v, 200L, 201L + v, visibleQty, visibleQty);
        }
        final VenueSessionState sessions = new VenueSessionState(CONFIG.venueCount());
        new VenueSessionSimulator(CONFIG).openAll(sessions);
        if (!venueOpen) {
            sessions.setStatus(venueId, VenueStatus.OUTAGE);
        }
        final VenueThrottleState throttles = new VenueThrottleState(CONFIG.venueCount());
        new VenueThrottleSimulator(CONFIG).populate(throttles);
        final RegimeState regimes = new RegimeState(CONFIG.instrumentCount());
        regimes.setRegime(0, regimeId);
        regimes.setRegime(1, regimeId);
        final ChildOrderBuffer orders = new ChildOrderBuffer(1);
        orders.add(1L, 1L, 0, venueId, Side.BUY, orderQty, 1L, 1L);
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(1);
        final ChildOrderState childState = new ChildOrderState(1);

        new VenueBehaviorSimulator(CONFIG, seed).process(
                orders, book, sessions, throttles, regimes, scenario, outcomes, outstanding, childState);
        return new VenueOutcomeRun(outcomes, outstanding, childState);
    }

    private static ScenarioRun runBookScenario(final long seed, final ScenarioSpec spec) {
        final ScenarioState scenario = ScenarioState.fresh(spec);
        final MarketBookState book = new MarketBookState(CONFIG.instrumentCount(), CONFIG.venueCount());
        final FeedHealthState feedHealth = new FeedHealthState(CONFIG.venueCount());
        final MarketDataSimulator simulator = new MarketDataSimulator(CONFIG, seed);
        long previousMid0 = simulator.currentMidTicks(0);
        long previousMid1 = simulator.currentMidTicks(1);
        long maxMidStep = 0L;
        long movement = 0L;
        long staleEventCount = 0L;
        long feedChecksum = 0L;

        for (int tick = 0; tick < spec.ticks(); tick++) {
            simulator.generateTick(book, scenario, feedHealth);
            final long mid0 = simulator.currentMidTicks(0);
            final long mid1 = simulator.currentMidTicks(1);
            maxMidStep = Math.max(maxMidStep, Math.abs(mid0 - previousMid0));
            maxMidStep = Math.max(maxMidStep, Math.abs(mid1 - previousMid1));
            movement += Math.abs(mid0 - previousMid0) + Math.abs(mid1 - previousMid1);
            previousMid0 = mid0;
            previousMid1 = mid1;
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                if (feedHealth.isStale(venueId)) {
                    staleEventCount++;
                }
                feedChecksum = feedChecksum * 31L + feedHealth.lastSequence(venueId) + (feedHealth.isStale(venueId) ? 7L : 0L);
            }
            scenario.clock().advance();
        }

        return new ScenarioRun(
                book,
                simulator,
                maxMidStep,
                movement,
                totalDisplayedQty(book),
                maxSameInstrumentVenueMidDistance(book),
                maxSameVenueCrossInstrumentMidDistance(book),
                simulator.currentMidTicks(0) * 31L + simulator.currentMidTicks(1),
                staleEventCount,
                feedChecksum
        );
    }

    private static void assertBookEquals(final MarketBookState first, final MarketBookState second) {
        assertEquals(first.sequence(), second.sequence());
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                assertEquals(first.bidPriceTicks(instrumentId, venueId), second.bidPriceTicks(instrumentId, venueId));
                assertEquals(first.askPriceTicks(instrumentId, venueId), second.askPriceTicks(instrumentId, venueId));
                assertEquals(first.bidQty(instrumentId, venueId), second.bidQty(instrumentId, venueId));
                assertEquals(first.askQty(instrumentId, venueId), second.askQty(instrumentId, venueId));
            }
        }
    }

    private static long totalDisplayedQty(final MarketBookState book) {
        long total = 0L;
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                total += book.bidQty(instrumentId, venueId) + book.askQty(instrumentId, venueId);
            }
        }
        return total;
    }

    private static long maxSameInstrumentVenueMidDistance(final MarketBookState book) {
        long max = 0L;
        for (int instrumentId = 0; instrumentId < CONFIG.instrumentCount(); instrumentId++) {
            long minMid = Long.MAX_VALUE;
            long maxMid = Long.MIN_VALUE;
            for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
                final long mid = (book.bidPriceTicks(instrumentId, venueId) + book.askPriceTicks(instrumentId, venueId)) / 2L;
                minMid = Math.min(minMid, mid);
                maxMid = Math.max(maxMid, mid);
            }
            max = Math.max(max, maxMid - minMid);
        }
        return max;
    }

    private static long maxSameVenueCrossInstrumentMidDistance(final MarketBookState book) {
        long max = 0L;
        for (int venueId = 0; venueId < CONFIG.venueCount(); venueId++) {
            final long firstMid = (book.bidPriceTicks(0, venueId) + book.askPriceTicks(0, venueId)) / 2L;
            final long secondMid = (book.bidPriceTicks(1, venueId) + book.askPriceTicks(1, venueId)) / 2L;
            max = Math.max(max, Math.abs(firstMid - secondMid));
        }
        return max;
    }

    private record ScenarioRun(
            MarketBookState book,
            MarketDataSimulator simulator,
            long maxMidStep,
            long totalAbsMidMovement,
            long totalDisplayedQty,
            long maxSameInstrumentVenueMidDistance,
            long maxSameVenueCrossInstrumentMidDistance,
            long midChecksum,
            long staleEventCount,
            long feedChecksum
    ) {
    }

    private record VenueOutcomeRun(
            ExecutionOutcomeStore outcomes,
            OutstandingChildOrderState outstanding,
            ChildOrderState childState
    ) {
    }
}
