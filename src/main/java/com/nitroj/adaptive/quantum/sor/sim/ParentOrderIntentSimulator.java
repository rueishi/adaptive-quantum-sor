package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

import java.util.Random;

/**
 * Responsibility: generate deterministic simulated parent order intents.
 *
 * <p>Role in system: stands in for a strategy or trader order stream during
 * Phase 1 simulations.</p>
 *
 * <p>Relationships: produces {@link OrderIntent} records that can be enqueued
 * into {@code ParentOrderIntentQueue} or passed to execution tests.</p>
 *
 * <p>Lifecycle: created with fixed config and seed, then called repeatedly to
 * produce a reproducible order sequence.</p>
 *
 * <p>Design intent: bounded IDs, quantities, instruments, sides, and urgency
 * values keep simulator output valid for downstream state structures.</p>
 */
public final class ParentOrderIntentSimulator {
    private final SorConfig config;
    private final Random random;
    private long nextParentOrderId = 1L;

    public ParentOrderIntentSimulator(final SorConfig config, final long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.random = new Random(seed);
    }

    /** Generates the next valid parent order intent in the deterministic sequence. */
    public OrderIntent next(final long createdAtNanos) {
        final int instrumentId = random.nextInt(config.instrumentCount());
        final int side = random.nextBoolean() ? Side.BUY : Side.SELL;
        final long quantity = 100L + random.nextInt(9_901);
        final int urgencyId = random.nextInt(config.urgencyCount());
        return new OrderIntent(nextParentOrderId++, instrumentId, side, quantity, urgencyId, createdAtNanos);
    }

    /**
     * Generates one scenario-conditioned parent order using simulated time.
     *
     * <p>Core logic: normal regimes keep the legacy quantity distribution,
     * volatile regimes produce larger and more urgent orders with a buy-side
     * imbalance, and thin-book regimes produce smaller less urgent orders. The
     * timestamp always comes from {@link ScenarioState#clock()}.</p>
     *
     * @param scenarioState current scenario context
     * @return deterministic parent order for the current scenario tick
     */
    public OrderIntent next(final ScenarioState scenarioState) {
        if (scenarioState == null) {
            throw new IllegalArgumentException("scenarioState must not be null");
        }
        final int regimeId = scenarioState.spec().regimeAt(scenarioState.clock().tick());
        final int instrumentId = random.nextInt(config.instrumentCount());
        final int side = sideForRegime(regimeId);
        final long quantity = quantityForRegime(regimeId);
        final int urgencyId = urgencyForRegime(regimeId);
        return new OrderIntent(nextParentOrderId++, instrumentId, side, quantity, urgencyId, scenarioState.clock().nowNanos());
    }

    /**
     * Emits the deterministic number of parent orders for the current scenario tick.
     *
     * <p>Core logic: volatile windows emit two parent orders, normal windows emit
     * one, and thin-book windows emit one only on even ticks. If the queue fills,
     * emission stops and the returned count reflects accepted orders only.</p>
     *
     * @param scenarioState current scenario context
     * @param queue destination parent-order queue
     * @return number of orders accepted into the queue
     */
    public int offerForCurrentTick(final ScenarioState scenarioState, final ParentOrderIntentQueue queue) {
        if (scenarioState == null || queue == null) {
            throw new IllegalArgumentException("scenarioState and queue must not be null");
        }
        final int regimeId = scenarioState.spec().regimeAt(scenarioState.clock().tick());
        final int targetCount;
        if (regimeId == RegimeState.VOLATILE) {
            targetCount = 2;
        } else if (regimeId == RegimeState.THIN_BOOK) {
            targetCount = scenarioState.clock().tick() % 2 == 0 ? 1 : 0;
        } else {
            targetCount = 1;
        }
        int accepted = 0;
        for (int i = 0; i < targetCount; i++) {
            if (!queue.offer(next(scenarioState))) {
                break;
            }
            accepted++;
        }
        return accepted;
    }

    private int sideForRegime(final int regimeId) {
        if (regimeId == RegimeState.VOLATILE) {
            return random.nextInt(10) < 7 ? Side.BUY : Side.SELL;
        }
        if (regimeId == RegimeState.THIN_BOOK) {
            return random.nextInt(10) < 6 ? Side.SELL : Side.BUY;
        }
        return random.nextBoolean() ? Side.BUY : Side.SELL;
    }

    private long quantityForRegime(final int regimeId) {
        if (regimeId == RegimeState.VOLATILE) {
            return 5_000L + random.nextInt(15_001);
        }
        if (regimeId == RegimeState.THIN_BOOK) {
            return 100L + random.nextInt(1_901);
        }
        return 100L + random.nextInt(9_901);
    }

    private int urgencyForRegime(final int regimeId) {
        if (regimeId == RegimeState.VOLATILE) {
            return Math.max(0, config.urgencyCount() - 1);
        }
        if (regimeId == RegimeState.THIN_BOOK) {
            return 0;
        }
        return random.nextInt(config.urgencyCount());
    }
}
