package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.feature.FeatureAggregator;
import com.nitroj.adaptive.quantum.sor.feature.RollingWindowConfig;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.adaptive.quantum.sor.sim.MarketDataSimulator;
import com.nitroj.adaptive.quantum.sor.sim.MarketSessionSimulator;
import com.nitroj.adaptive.quantum.sor.sim.ParentOrderIntentSimulator;
import com.nitroj.adaptive.quantum.sor.sim.SimulatorHealthState;
import com.nitroj.adaptive.quantum.sor.sim.VenueBehaviorSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueSessionSimulator;
import com.nitroj.adaptive.quantum.sor.sim.VenueThrottleSimulator;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.FeedHealthState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.FillQualityStats;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;
import com.nitroj.adaptive.quantum.sor.stats.SlippageStats;
import com.nitroj.adaptive.quantum.sor.stats.ToxicityStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueHealthStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueLatencyStats;
import com.nitroj.adaptive.quantum.sor.stats.VenueStatsState;

/**
 * Responsibility: execute a replayable scenario specification.
 *
 * <p>Role in system: P5-TC-001 establishes this canonical JUnit entry point.
 * Later task cards wire low-level simulators into the run; this initial version
 * validates deterministic metadata, clock, profile, and random stream contracts.</p>
 *
 * <p>Relationships: consumes {@link ScenarioSpec}, creates
 * {@link ScenarioState}, and returns {@link ScenarioSummary}.</p>
 *
 * <p>Lifecycle: stateless runner created by tests, CLI, or future API code.</p>
 *
 * <p>Design intent: keep orchestration separate from low-level `sim` classes so
 * deterministic simulator tests and scenario-driven tests remain distinct.</p>
 */
public final class ScenarioRunner {
    /**
     * Runs the metadata-level scenario contract and returns a deterministic
     * summary. Later Phase 5 task cards enrich this loop with market, order,
     * venue, feature, optimizer, and routing state.
     */
    public ScenarioSummary run(final ScenarioSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }
        final ScenarioState state = ScenarioState.fresh(spec);
        long checksum = spec.seed() ^ spec.scenarioId().hashCode();
        int normal = 0;
        int volatileCount = 0;
        int thinBook = 0;
        int staleEvents = 0;
        int orderCount = 0;
        int childOrderCount = 0;
        long residualQty = 0L;
        final MarketBookState marketBooks = new MarketBookState(spec.config().instrumentCount(), spec.config().venueCount());
        final FeedHealthState feedHealth = new FeedHealthState(spec.config().venueCount());
        final MarketSessionState marketSessions = new MarketSessionState(spec.config().instrumentCount());
        final VenueSessionState venueSessions = new VenueSessionState(spec.config().venueCount());
        final VenueThrottleState throttles = new VenueThrottleState(spec.config().venueCount());
        final RegimeState regimes = new RegimeState(spec.config().instrumentCount());
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(Math.max(16, spec.ticks() * 8));
        final OutstandingChildOrderState outstanding = new OutstandingChildOrderState(Math.max(1, spec.ticks() * 2));
        final ChildOrderState childState = new ChildOrderState(Math.max(1, spec.ticks() * 2));
        final MarketDataSimulator marketData = new MarketDataSimulator(spec.config(), spec.seed());
        final MarketSessionSimulator marketSessionSimulator = new MarketSessionSimulator(spec.config());
        final VenueSessionSimulator venueSessionSimulator = new VenueSessionSimulator(spec.config());
        final VenueThrottleSimulator throttleSimulator = new VenueThrottleSimulator(spec.config());
        final ParentOrderIntentSimulator orderSimulator = new ParentOrderIntentSimulator(spec.config(), spec.seed() ^ 0x504f524445524cL);
        final VenueBehaviorSimulator venueBehavior = new VenueBehaviorSimulator(spec.config(), spec.seed() ^ 0x56424cL);
        final ChildOrderBuffer childOrders = new ChildOrderBuffer(2);
        for (int tick = 0; tick < spec.ticks(); tick++) {
            final int regime = spec.regimeAt(tick);
            for (int instrumentId = 0; instrumentId < spec.config().instrumentCount(); instrumentId++) {
                regimes.setRegime(instrumentId, regime);
            }
            marketSessionSimulator.applyScenario(marketSessions, state);
            venueSessionSimulator.applyScenario(venueSessions, state);
            throttleSimulator.populate(throttles, state);
            marketData.generateTick(marketBooks, state, feedHealth);
            checksum = checksum * 31 + regime;
            checksum = checksum * 31 + bookChecksum(marketBooks, spec.config().instrumentCount(), spec.config().venueCount());
            if (regime == 0) {
                normal++;
            } else if (regime == 1) {
                volatileCount++;
            } else if (regime == 2) {
                thinBook++;
            }
            for (int venueId = 0; venueId < spec.config().venueCount(); venueId++) {
                if (feedHealth.isStale(venueId)) {
                    staleEvents++;
                }
            }
            childOrders.reset();
            final OrderIntent parent = orderSimulator.next(state);
            orderCount++;
            final int venueId = Math.floorMod(tick, spec.config().venueCount());
            childOrders.add(tick + 1L, parent.parentOrderId, parent.instrumentId, venueId, parent.side,
                    Math.min(parent.quantity, 2_000L), 1L, 1L);
            childOrderCount += childOrders.size();
            final int outcomeStart = outcomes.size();
            venueBehavior.process(childOrders, marketBooks, venueSessions, throttles, regimes, state, outcomes, outstanding, childState);
            for (int outcome = outcomeStart; outcome < outcomes.size(); outcome++) {
                checksum = checksum * 31L + outcomes.outcomeType(outcome) + outcomes.filledQty(outcome);
            }
            residualQty += outstanding.remainingQty(0);
            state.clock().advance();
        }
        final VenueStatsState venueStats = new VenueStatsState(spec.config().instrumentCount(), spec.config().venueCount(), spec.config().regimeCount());
        final VenueLatencyStats latencyStats = new VenueLatencyStats(spec.config().venueCount());
        final FillQualityStats fillStats = new FillQualityStats(spec.config().venueCount());
        final ToxicityStats toxicityStats = new ToxicityStats(spec.config().venueCount());
        final SlippageStats slippageStats = new SlippageStats(spec.config().venueCount());
        final VenueHealthStats healthStats = new VenueHealthStats(spec.config().venueCount());
        final RegimeState detectedRegimes = new RegimeState(spec.config().instrumentCount());
        new FeatureAggregator(spec.config().instrumentCount(), spec.config().venueCount(), spec.config().regimeCount(),
                new RollingWindowConfig(Math.max(1, outcomes.size()), 8_500L, 12L)).aggregate(
                marketBooks, outcomes, venueStats, latencyStats, fillStats, toxicityStats, slippageStats, healthStats, detectedRegimes);
        return new ScenarioSummary(
                spec.scenarioId(),
                spec.seed(),
                spec.ticks(),
                checksum,
                orderCount,
                childOrderCount,
                outcomes.size(),
                childOrderCount,
                healthStats.rejectRateBps(0) > 0 ? 1 : 0,
                0,
                0,
                residualQty,
                normal,
                volatileCount * 2L,
                spec.simulatorConfig().normalDisplayedQty(),
                spec.simulatorConfig().thinBookDisplayedQty(),
                staleEvents,
                0,
                normal,
                volatileCount,
                thinBook
        );
    }

    /**
     * Builds compact optimizer input lineage from a completed scenario summary.
     *
     * <p>If simulator health is unhealthy, this method returns {@code null} so
     * callers cannot accidentally publish optimizer inputs from partial state.</p>
     */
    public PolicyOptimizationInput optimizerInputLineage(
            final ScenarioSummary summary,
            final ScenarioSpec spec,
            final SimulatorHealthState simulatorHealth
    ) {
        if (summary == null || spec == null || simulatorHealth == null) {
            throw new IllegalArgumentException("summary, spec, and simulatorHealth must not be null");
        }
        if (!simulatorHealth.healthy()) {
            return null;
        }
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = Math.abs(summary.bookChecksum());
        input.createdAtNanos = summary.ticksRun();
        input.instrumentCount = spec.config().instrumentCount();
        input.venueCount = spec.config().venueCount();
        input.regimeCount = spec.config().regimeCount();
        input.urgencyCount = spec.config().urgencyCount();
        input.marketDataSnapshotSeq = summary.bookChecksum();
        input.executionOutcomeSnapshotSeq = summary.outcomeCount();
        input.scenarioId = summary.scenarioId();
        input.scenarioSeed = summary.seed();
        input.scenarioTicks = summary.ticksRun();
        input.scenarioStartTickInclusive = 0;
        input.scenarioEndTickInclusive = Math.max(0, summary.ticksRun() - 1);
        input.scenarioInputPublishable = true;
        return input;
    }

    private static long bookChecksum(final MarketBookState book, final int instrumentCount, final int venueCount) {
        long checksum = 17L;
        for (int instrumentId = 0; instrumentId < instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < venueCount; venueId++) {
                checksum = checksum * 31L + book.bidPriceTicks(instrumentId, venueId);
                checksum = checksum * 31L + book.askPriceTicks(instrumentId, venueId);
                checksum = checksum * 31L + book.bidQty(instrumentId, venueId);
                checksum = checksum * 31L + book.askQty(instrumentId, venueId);
            }
        }
        return checksum;
    }
}
