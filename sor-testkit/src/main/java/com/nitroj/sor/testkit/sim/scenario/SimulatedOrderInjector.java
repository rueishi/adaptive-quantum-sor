package com.nitroj.sor.testkit.sim.scenario;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Responsibility: deterministic parent-order generation and scenario injection.
 *
 * <p>Role in system: replaces legacy parent-order intent simulation using the
 * public {@link ParentOrderRequest} DTO and simulator-local descriptors.</p>
 *
 * <p>Relationships: new scenario runners call this component to generate
 * baseline, volatile, thin-book, and explicit scheduled parent orders.</p>
 *
 * <p>Lifecycle: constructed with config/seed at scenario setup and advanced
 * once per simulated tick.</p>
 *
 * <p>Design intent: preserve fixed-seed order distributions while submitting
 * through the engine API instead of a legacy queue.</p>
 */
public final class SimulatedOrderInjector {
    private final SimConfig config;
    private final Random random;
    private long nextParentOrderId = 1L;

    public SimulatedOrderInjector() {
        this(SimConfig.defaults(), 0L);
    }

    public SimulatedOrderInjector(final SimConfig config, final long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.random = new Random(seed);
    }

    public ParentOrderRequest request(final int instrumentId, final long quantity) {
        return ParentOrderRequest.builder().instrumentId(instrumentId).side(Side.BUY).quantity(quantity).urgency(0).build();
    }

    /** Generates the next legacy-equivalent normal-regime parent order. */
    public SimParentOrder next(final long createdAtNanos) {
        final int instrumentId = random.nextInt(config.instrumentCount());
        final int side = random.nextBoolean() ? Side.BUY : Side.SELL;
        final long quantity = 100L + random.nextInt(9_901);
        final int urgencyId = random.nextInt(config.urgencyCount());
        return new SimParentOrder(nextParentOrderId++, instrumentId, side, quantity, urgencyId, 0L, createdAtNanos);
    }

    /** Generates one regime-conditioned parent order using legacy formulas. */
    public SimParentOrder nextForRegime(final int regimeId, final long createdAtNanos) {
        final int instrumentId = random.nextInt(config.instrumentCount());
        final int side = sideForRegime(regimeId);
        final long quantity = quantityForRegime(regimeId);
        final int urgencyId = urgencyForRegime(regimeId);
        return new SimParentOrder(nextParentOrderId++, instrumentId, side, quantity, urgencyId, 0L, createdAtNanos);
    }

    /** Emits deterministic parent orders for a scenario tick. */
    public List<SimParentOrder> ordersForTick(
            final int tick,
            final int regimeId,
            final long createdAtNanos,
            final List<SimScenarioParentOrder> scheduled
    ) {
        final ArrayList<SimParentOrder> orders = new ArrayList<>(2);
        if (scheduled != null) {
            for (SimScenarioParentOrder explicit : scheduled) {
                if (explicit.atTick() == tick) {
                    orders.add(new SimParentOrder(nextParentOrderId++, explicit.instrumentId(), explicit.side(),
                            explicit.quantity(), explicit.urgencyId(), explicit.clientOrderRef(), createdAtNanos));
                }
            }
        }
        if (!orders.isEmpty()) {
            return orders;
        }
        final int targetCount;
        if (regimeId == SimRegime.VOLATILE) {
            targetCount = 2;
        } else if (regimeId == SimRegime.THIN_BOOK) {
            targetCount = tick % 2 == 0 ? 1 : 0;
        } else {
            targetCount = 1;
        }
        for (int i = 0; i < targetCount; i++) {
            orders.add(nextForRegime(regimeId, createdAtNanos));
        }
        return orders;
    }

    /** Converts a simulator-local order into the public engine submission DTO. */
    public ParentOrderRequest toRequest(final SimParentOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        return ParentOrderRequest.builder()
                .instrumentId(order.instrumentId())
                .side(order.side())
                .quantity(order.quantity())
                .urgency(order.urgencyId())
                .clientOrderId(order.clientOrderRef())
                .arrivalEpochNanos(order.arrivalEpochNanos())
                .build();
    }

    private int sideForRegime(final int regimeId) {
        if (regimeId == SimRegime.VOLATILE) {
            return random.nextInt(10) < 7 ? Side.BUY : Side.SELL;
        }
        if (regimeId == SimRegime.THIN_BOOK) {
            return random.nextInt(10) < 6 ? Side.SELL : Side.BUY;
        }
        return random.nextBoolean() ? Side.BUY : Side.SELL;
    }

    private long quantityForRegime(final int regimeId) {
        if (regimeId == SimRegime.VOLATILE) {
            return 5_000L + random.nextInt(15_001);
        }
        if (regimeId == SimRegime.THIN_BOOK) {
            return 100L + random.nextInt(1_901);
        }
        return 100L + random.nextInt(9_901);
    }

    private int urgencyForRegime(final int regimeId) {
        if (regimeId == SimRegime.VOLATILE) {
            return Math.max(0, config.urgencyCount() - 1);
        }
        if (regimeId == SimRegime.THIN_BOOK) {
            return 0;
        }
        return random.nextInt(config.urgencyCount());
    }
}
