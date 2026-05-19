package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.ChildOrder;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioVenueProfile;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.VenueBehaviorState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

import java.util.Random;

/**
 * Responsibility: simulate ACK, fill, reject, and cancel-style venue outcomes.
 *
 * <p>Role in system: replaces real exchange matching behavior for Phase 1 and
 * supplies raw outcomes for feature aggregation.</p>
 *
 * <p>Relationships: reads {@link ChildOrderBuffer}, writes
 * {@link ExecutionOutcomeStore}, {@link ChildOrderState}, and
 * {@link OutstandingChildOrderState}; owns a {@link VenueBehaviorState}
 * profile surface.</p>
 *
 * <p>Lifecycle: initialized with deterministic profiles and seed, then called
 * whenever generated child orders need simulated venue responses.</p>
 *
 * <p>Design intent: profile values create fast, slow, toxic, reject-prone, and
 * liquidity-fading behavior without modeling a full exchange.</p>
 */
public final class VenueBehaviorSimulator {
    private final SorConfig config;
    private final Random random;
    private final VenueBehaviorState behaviorState;

    public VenueBehaviorSimulator(final SorConfig config, final long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.random = new Random(seed);
        this.behaviorState = new VenueBehaviorState(config.venueCount());
        initializeProfiles();
    }

    /** Returns the deterministic behavior profile state. */
    public VenueBehaviorState behaviorState() {
        return behaviorState;
    }

    /**
     * Processes active child orders and appends at least one outcome per order.
     */
    public void process(
            final ChildOrderBuffer childOrders,
            final ExecutionOutcomeStore outcomes,
            final OutstandingChildOrderState outstanding,
            final ChildOrderState childOrderState
    ) {
        if (childOrders == null || outcomes == null || outstanding == null || childOrderState == null) {
            throw new IllegalArgumentException("venue behavior inputs must not be null");
        }
        for (int slot = 0; slot < childOrders.size(); slot++) {
            final ChildOrder order = childOrders.get(slot);
            outstanding.put(slot, order.childOrderId, order.quantity);
            outcomes.append(order.childOrderId, order.venueId, ExecutionOutcomeStore.ACK, 0L, latencyNanos(order.venueId), 0, 0);
            if (random.nextInt(10_000) < behaviorStateRejectRate(order.venueId)) {
                outcomes.append(order.childOrderId, order.venueId, ExecutionOutcomeStore.REJECT, 0L, latencyNanos(order.venueId), 0, behaviorState.toxicityBps(order.venueId));
                childOrderState.update(slot, OrderStatus.REJECTED, 0L);
            } else {
                final long fillQty = Math.max(1L, order.quantity * behaviorStateFillProbability(order.venueId) / 10_000L);
                final int fillType = fillQty >= order.quantity ? ExecutionOutcomeStore.FULL_FILL : ExecutionOutcomeStore.PARTIAL_FILL;
                outcomes.append(order.childOrderId, order.venueId, fillType, fillQty, latencyNanos(order.venueId), random.nextInt(50), behaviorState.toxicityBps(order.venueId));
                outstanding.applyFill(slot, fillQty);
                childOrderState.update(slot, fillType == ExecutionOutcomeStore.FULL_FILL ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED, fillQty);
            }
        }
    }

    /**
     * Processes child orders using current scenario market/session/regime state.
     *
     * <p>Core logic: every child receives an ACK, unavailable or throttled
     * venues reject without fills, and executable venues derive fill quantity
     * from displayed liquidity, order size, venue profile, toxicity, and active
     * regime. Fill quantities are bounded by both child quantity and visible
     * liquidity before publication to {@link ExecutionOutcomeStore}.</p>
     *
     * @param childOrders active child orders to simulate
     * @param marketBookState current top-of-book liquidity
     * @param venueSessions current venue executable state
     * @param throttleState current venue throttle limits
     * @param regimeState optional per-instrument regime state
     * @param scenarioState scenario profile and clock context
     * @param outcomes destination outcome store
     * @param outstanding outstanding child-order state
     * @param childOrderState child lifecycle state
     */
    public void process(
            final ChildOrderBuffer childOrders,
            final MarketBookState marketBookState,
            final VenueSessionState venueSessions,
            final VenueThrottleState throttleState,
            final RegimeState regimeState,
            final ScenarioState scenarioState,
            final ExecutionOutcomeStore outcomes,
            final OutstandingChildOrderState outstanding,
            final ChildOrderState childOrderState
    ) {
        if (childOrders == null || marketBookState == null || venueSessions == null || throttleState == null
                || scenarioState == null || outcomes == null || outstanding == null || childOrderState == null) {
            throw new IllegalArgumentException("scenario venue behavior inputs must not be null");
        }
        for (int slot = 0; slot < childOrders.size(); slot++) {
            final ChildOrder order = childOrders.get(slot);
            final int regimeId = regimeState == null
                    ? scenarioState.spec().regimeAt(scenarioState.clock().tick())
                    : regimeState.regime(order.instrumentId);
            final ScenarioVenueProfile profile = scenarioState.profile(order.venueId);
            final int latency = latencyNanos(order.venueId, profile, regimeId);
            outstanding.put(slot, order.childOrderId, order.quantity);
            outcomes.append(order.childOrderId, order.venueId, ExecutionOutcomeStore.ACK, 0L, latency, 0, 0);

            if (!venueSessions.isAvailable(order.venueId) || throttleState.maxOrderRatePerSecond(order.venueId) == 0) {
                appendReject(order, slot, outcomes, outstanding, childOrderState, latency, profile, regimeId);
                continue;
            }

            final long visibleQty = visibleQty(marketBookState, order);
            final int rejectBps = rejectBps(order.venueId, profile, regimeId, visibleQty, order.quantity);
            if (visibleQty <= 0 || random.nextInt(10_000) < rejectBps) {
                appendReject(order, slot, outcomes, outstanding, childOrderState, latency, profile, regimeId);
                continue;
            }

            final long fillQty = boundedFillQty(order.quantity, visibleQty, profile, regimeId);
            final int fillType = fillQty >= order.quantity ? ExecutionOutcomeStore.FULL_FILL : ExecutionOutcomeStore.PARTIAL_FILL;
            outcomes.append(order.childOrderId, order.venueId, fillType, fillQty, latency,
                    slippageBps(marketBookState, order, profile, regimeId), toxicityBps(profile, regimeId));
            outstanding.applyFill(slot, fillQty);
            childOrderState.update(slot,
                    fillType == ExecutionOutcomeStore.FULL_FILL ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED,
                    fillQty);
        }
    }

    private void initializeProfiles() {
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final int profile = venueId % 5;
            if (profile == 0) {
                behaviorState.update(venueId, 100, 50, 9_000);
            } else if (profile == 1) {
                behaviorState.update(venueId, 200, 100, 8_000);
            } else if (profile == 2) {
                behaviorState.update(venueId, 2_500, 150, 7_500);
            } else if (profile == 3) {
                behaviorState.update(venueId, 300, 2_000, 6_500);
            } else {
                behaviorState.update(venueId, 800, 400, 5_500);
            }
        }
    }

    private int latencyNanos(final int venueId) {
        return (venueId % 5 == 1 ? 5_000_000 : 750_000) + random.nextInt(100_000);
    }

    private int latencyNanos(final int venueId, final ScenarioVenueProfile profile, final int regimeId) {
        int latency = (venueId % 5 == 1 ? 5_000_000 : 750_000) + random.nextInt(100_000);
        if (profile == ScenarioVenueProfile.WIDE_SLOW) {
            latency += 2_000_000;
        } else if (profile == ScenarioVenueProfile.OUTAGE_PRONE) {
            latency += 750_000;
        }
        if (regimeId == RegimeState.VOLATILE) {
            latency += 1_500_000;
        } else if (regimeId == RegimeState.THIN_BOOK) {
            latency += 500_000;
        }
        return latency;
    }

    private int behaviorStateRejectRate(final int venueId) {
        return venueId % 5 == 3 ? 2_000 : 100 + venueId % 7;
    }

    private int behaviorStateFillProbability(final int venueId) {
        return venueId % 5 == 4 ? 5_500 : 8_000;
    }

    private void appendReject(
            final ChildOrder order,
            final int slot,
            final ExecutionOutcomeStore outcomes,
            final OutstandingChildOrderState outstanding,
            final ChildOrderState childOrderState,
            final int latency,
            final ScenarioVenueProfile profile,
            final int regimeId
    ) {
        outcomes.append(order.childOrderId, order.venueId, ExecutionOutcomeStore.REJECT, 0L, latency, 0,
                toxicityBps(profile, regimeId));
        outstanding.applyFill(slot, 0L);
        childOrderState.update(slot, OrderStatus.REJECTED, 0L);
    }

    private long visibleQty(final MarketBookState marketBookState, final ChildOrder order) {
        return order.side == Side.BUY
                ? marketBookState.askQty(order.instrumentId, order.venueId)
                : marketBookState.bidQty(order.instrumentId, order.venueId);
    }

    private long boundedFillQty(
            final long orderQty,
            final long visibleQty,
            final ScenarioVenueProfile profile,
            final int regimeId
    ) {
        int probability = behaviorStateFillProbability(profile.ordinal());
        if (profile == ScenarioVenueProfile.TIGHT_DEEP) {
            probability = 9_000;
        } else if (profile == ScenarioVenueProfile.TOXIC) {
            probability = 6_000;
        } else if (profile == ScenarioVenueProfile.OUTAGE_PRONE) {
            probability = 5_000;
        }
        if (regimeId == RegimeState.THIN_BOOK) {
            probability = Math.min(probability, 4_500);
        } else if (regimeId == RegimeState.VOLATILE) {
            probability = Math.min(probability, 7_000);
        }
        final long probabilisticFill = Math.max(1L, orderQty * probability / 10_000L);
        return Math.min(orderQty, Math.min(visibleQty, probabilisticFill));
    }

    private int rejectBps(
            final int venueId,
            final ScenarioVenueProfile profile,
            final int regimeId,
            final long visibleQty,
            final long orderQty
    ) {
        int reject = behaviorStateRejectRate(venueId);
        if (profile == ScenarioVenueProfile.OUTAGE_PRONE) {
            reject += 1_000;
        } else if (profile == ScenarioVenueProfile.STALE_FEED) {
            reject += 500;
        }
        if (visibleQty < orderQty) {
            reject += 1_500;
        }
        if (regimeId == RegimeState.VOLATILE) {
            reject += 750;
        } else if (regimeId == RegimeState.THIN_BOOK) {
            reject += 1_000;
        }
        return Math.min(9_500, Math.max(0, reject));
    }

    private int slippageBps(final MarketBookState marketBookState, final ChildOrder order, final ScenarioVenueProfile profile, final int regimeId) {
        final long spread = marketBookState.askPriceTicks(order.instrumentId, order.venueId)
                - marketBookState.bidPriceTicks(order.instrumentId, order.venueId);
        int slippage = (int) Math.min(10_000L, spread * 10L);
        if (profile == ScenarioVenueProfile.TOXIC) {
            slippage += 250;
        } else if (profile == ScenarioVenueProfile.WIDE_SLOW) {
            slippage += 100;
        }
        if (regimeId == RegimeState.VOLATILE) {
            slippage += 150;
        } else if (regimeId == RegimeState.THIN_BOOK) {
            slippage += 75;
        }
        return Math.min(10_000, slippage);
    }

    private int toxicityBps(final ScenarioVenueProfile profile, final int regimeId) {
        int toxicity = profile == ScenarioVenueProfile.TOXIC ? 2_500 : profile == ScenarioVenueProfile.OUTAGE_PRONE ? 800 : 100;
        if (regimeId == RegimeState.VOLATILE) {
            toxicity += 300;
        }
        return Math.min(10_000, toxicity);
    }
}
