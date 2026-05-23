package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.ChildOrderRef;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.RejectReport;
import com.nitroj.sor.api.spi.RingWriter;
import com.nitroj.sor.api.spi.VenueAdapter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Responsibility: deterministic venue gateway simulator.
 *
 * <p>Role in system: implements the {@link VenueAdapter} ring-writer contract
 * and converts queued child orders into ACK, fill, and reject outcomes under
 * deterministic session, throttle, liquidity, slippage, toxicity, and latency
 * rules.</p>
 *
 * <p>Relationships: receives child orders from {@link RingWriter}, reads
 * simulator-local market/session/throttle snapshots, and emits SPI callbacks
 * to the engine or simulator-local {@link SimVenueOutcome} rows to tests.</p>
 *
 * <p>Lifecycle: constructed with a clock and optional seed, wired with an
 * engine callback, then polled explicitly by the scenario runner.</p>
 *
 * <p>Design intent: preserve the legacy venue formulas while keeping routing
 * threads limited to ring writes and moving outcome generation to simulator
 * polling.</p>
 */
public final class SimulatedVenueAdapter implements VenueAdapter {
    private final Map<Integer, VenueRing> rings = new ConcurrentHashMap<>();
    private final AtomicReference<VenueAdapterCallback> callback = new AtomicReference<>();
    private final ManualClock clock;
    private final SimConfig config;
    private final Random random;
    private volatile String lastConsumerThreadName = "";

    public SimulatedVenueAdapter(final ManualClock clock) {
        this(clock, SimConfig.defaults(), 0L);
    }

    public SimulatedVenueAdapter(final ManualClock clock, final SimConfig config, final long seed) {
        if (clock == null || config == null) {
            throw new IllegalArgumentException("clock and config must not be null");
        }
        this.clock = clock;
        this.config = config;
        this.random = new Random(seed);
    }

    @Override
    public RingWriter childOrderRingWriter(final int venueId) {
        return rings.computeIfAbsent(venueId, ignored -> new VenueRing());
    }

    @Override
    public void callback(final VenueAdapterCallback callback) {
        this.callback.set(callback);
    }

    public int pollVenue(final int venueId, final int maxOrders) {
        final VenueRing ring = rings.computeIfAbsent(venueId, ignored -> new VenueRing());
        int processed = 0;
        lastConsumerThreadName = Thread.currentThread().getName();
        while (processed < maxOrders) {
            final ChildOrderRef order = ring.poll();
            if (order == null) {
                break;
            }
            final VenueAdapterCallback cb = callback.get();
            if (cb != null) {
                cb.deliverFill(new FillReport().set(order.childOrderId(), order.parentOrderId(), order.venueId(),
                        order.quantity(), order.limitPrice(), clock.epochNanos()));
            }
            processed++;
        }
        return processed;
    }

    /** Returns all-open venue session state equivalent to the legacy helper. */
    public SimVenueSessionSnapshot openAllSessions() {
        final SimVenueSessionSnapshot snapshot = new SimVenueSessionSnapshot(config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            snapshot.setStatus(venueId, com.nitroj.sor.api.VenueStatus.OPEN);
        }
        return snapshot;
    }

    /** Applies the deterministic every-fifth-venue outage pattern. */
    public SimVenueSessionSnapshot outagePattern() {
        final SimVenueSessionSnapshot snapshot = new SimVenueSessionSnapshot(config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            snapshot.setStatus(venueId, venueId % 5 == 4
                    ? com.nitroj.sor.api.VenueStatus.HALTED
                    : com.nitroj.sor.api.VenueStatus.OPEN);
        }
        return snapshot;
    }

    /** Applies scenario outage windows for outage-prone venue profiles. */
    public void applyVenueSession(final SimVenueSessionSnapshot snapshot, final int regimeId, final int scenarioTick) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        final int outageCycle = regimeId == SimRegime.VOLATILE ? 6 : 10;
        final int outageLength = regimeId == SimRegime.VOLATILE ? 3 : 2;
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final SimVenueProfile profile = SimVenueProfile.forVenue(venueId);
            final boolean outage = profile == SimVenueProfile.OUTAGE_PRONE
                    && Math.floorMod(scenarioTick, outageCycle) < outageLength;
            snapshot.setStatus(venueId, outage
                    ? com.nitroj.sor.api.VenueStatus.HALTED
                    : com.nitroj.sor.api.VenueStatus.OPEN);
        }
    }

    /** Populates baseline venue throttle rates. */
    public SimVenueThrottleSnapshot populateThrottle() {
        final SimVenueThrottleSnapshot snapshot = new SimVenueThrottleSnapshot(config.venueCount());
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            snapshot.setMaxOrderRatePerSecond(venueId, 50 + venueId * 5);
        }
        return snapshot;
    }

    /** Populates scenario-aware throttle rates. */
    public void populateThrottle(final SimVenueThrottleSnapshot snapshot, final int regimeId) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            int rate = 50 + venueId * 5;
            if (regimeId == SimRegime.VOLATILE) {
                rate /= 2;
            } else if (regimeId == SimRegime.THIN_BOOK) {
                rate /= 3;
            }
            if (SimVenueProfile.forVenue(venueId) == SimVenueProfile.OUTAGE_PRONE) {
                rate = Math.min(rate, 20);
            }
            snapshot.setMaxOrderRatePerSecond(venueId, rate);
        }
    }

    /** Processes one simulator-local child order and returns ACK plus terminal outcome. */
    public List<SimVenueOutcome> process(
            final SimChildOrder order,
            final SimMarketBookSnapshot marketBook,
            final SimVenueSessionSnapshot venueSessions,
            final SimVenueThrottleSnapshot throttles,
            final int regimeId
    ) {
        if (order == null || marketBook == null || venueSessions == null || throttles == null) {
            throw new IllegalArgumentException("venue behavior inputs must not be null");
        }
        final ArrayList<SimVenueOutcome> outcomes = new ArrayList<>(2);
        final SimVenueProfile profile = SimVenueProfile.forVenue(order.venueId());
        final long latency = latencyNanos(order.venueId(), profile, regimeId);
        outcomes.add(new SimVenueOutcome(order.childOrderId(), order.parentOrderId(), order.venueId(),
                "ACK", 0L, latency, 0, 0));
        if (!venueSessions.isAvailable(order.venueId()) || throttles.maxOrderRatePerSecond(order.venueId()) == 0) {
            outcomes.add(reject(order, latency, profile, regimeId));
            return outcomes;
        }
        final long visibleQty = visibleQty(marketBook, order);
        final int rejectBps = rejectBps(order.venueId(), profile, regimeId, visibleQty, order.quantity());
        if (visibleQty <= 0 || random.nextInt(10_000) < rejectBps) {
            outcomes.add(reject(order, latency, profile, regimeId));
            return outcomes;
        }
        final long fillQty = boundedFillQty(order.quantity(), visibleQty, profile, regimeId);
        outcomes.add(new SimVenueOutcome(order.childOrderId(), order.parentOrderId(), order.venueId(),
                fillQty >= order.quantity() ? "FULL_FILL" : "PARTIAL_FILL",
                fillQty, latency, slippageBps(marketBook, order, profile, regimeId), toxicityBps(profile, regimeId)));
        return outcomes;
    }

    public void reject(final long childOrderId, final long parentOrderId, final int venueId, final int reasonCode) {
        final VenueAdapterCallback cb = callback.get();
        if (cb != null) {
            cb.deliverReject(new RejectReport().set(childOrderId, parentOrderId, venueId, reasonCode, clock.epochNanos()));
        }
    }

    public String lastConsumerThreadName() {
        return lastConsumerThreadName;
    }

    private SimVenueOutcome reject(final SimChildOrder order, final long latency,
                                   final SimVenueProfile profile, final int regimeId) {
        return new SimVenueOutcome(order.childOrderId(), order.parentOrderId(), order.venueId(),
                "REJECT", 0L, latency, 0, toxicityBps(profile, regimeId));
    }

    private long visibleQty(final SimMarketBookSnapshot marketBook, final SimChildOrder order) {
        return order.side() == com.nitroj.sor.api.Side.BUY
                ? marketBook.askQty(order.instrumentId(), order.venueId())
                : marketBook.bidQty(order.instrumentId(), order.venueId());
    }

    private long boundedFillQty(final long orderQty, final long visibleQty,
                                final SimVenueProfile profile, final int regimeId) {
        int probability = behaviorStateFillProbability(profile.ordinal());
        if (profile == SimVenueProfile.TIGHT_DEEP) {
            probability = 9_000;
        } else if (profile == SimVenueProfile.TOXIC) {
            probability = 6_000;
        } else if (profile == SimVenueProfile.OUTAGE_PRONE) {
            probability = 5_000;
        }
        if (regimeId == SimRegime.THIN_BOOK) {
            probability = Math.min(probability, 4_500);
        } else if (regimeId == SimRegime.VOLATILE) {
            probability = Math.min(probability, 7_000);
        }
        final long probabilisticFill = Math.max(1L, orderQty * probability / 10_000L);
        return Math.min(orderQty, Math.min(visibleQty, probabilisticFill));
    }

    private int rejectBps(final int venueId, final SimVenueProfile profile, final int regimeId,
                          final long visibleQty, final long orderQty) {
        int reject = behaviorStateRejectRate(venueId);
        if (profile == SimVenueProfile.OUTAGE_PRONE) {
            reject += 1_000;
        } else if (profile == SimVenueProfile.STALE_FEED) {
            reject += 500;
        }
        if (visibleQty < orderQty) {
            reject += 1_500;
        }
        if (regimeId == SimRegime.VOLATILE) {
            reject += 750;
        } else if (regimeId == SimRegime.THIN_BOOK) {
            reject += 1_000;
        }
        return Math.min(9_500, Math.max(0, reject));
    }

    private int slippageBps(final SimMarketBookSnapshot marketBook, final SimChildOrder order,
                            final SimVenueProfile profile, final int regimeId) {
        final long spread = marketBook.askPriceTicks(order.instrumentId(), order.venueId())
                - marketBook.bidPriceTicks(order.instrumentId(), order.venueId());
        int slippage = (int) Math.min(10_000L, spread * 10L);
        if (profile == SimVenueProfile.TOXIC) {
            slippage += 250;
        } else if (profile == SimVenueProfile.WIDE_SLOW) {
            slippage += 100;
        }
        if (regimeId == SimRegime.VOLATILE) {
            slippage += 150;
        } else if (regimeId == SimRegime.THIN_BOOK) {
            slippage += 75;
        }
        return Math.min(10_000, slippage);
    }

    private int toxicityBps(final SimVenueProfile profile, final int regimeId) {
        int toxicity = profile == SimVenueProfile.TOXIC ? 2_500 : profile == SimVenueProfile.OUTAGE_PRONE ? 800 : 100;
        if (regimeId == SimRegime.VOLATILE) {
            toxicity += 300;
        }
        return Math.min(10_000, toxicity);
    }

    private int latencyNanos(final int venueId, final SimVenueProfile profile, final int regimeId) {
        int latency = (venueId % 5 == 1 ? 5_000_000 : 750_000) + random.nextInt(100_000);
        if (profile == SimVenueProfile.WIDE_SLOW) {
            latency += 2_000_000;
        } else if (profile == SimVenueProfile.OUTAGE_PRONE) {
            latency += 750_000;
        }
        if (regimeId == SimRegime.VOLATILE) {
            latency += 1_500_000;
        } else if (regimeId == SimRegime.THIN_BOOK) {
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

    private static final class VenueRing implements RingWriter {
        private static final int CAPACITY = 1024;
        private final ArrayDeque<ChildOrderRef> orders = new ArrayDeque<>(CAPACITY);

        @Override
        public synchronized boolean offer(final ChildOrderRef childOrder) {
            if (orders.size() >= CAPACITY) {
                return false;
            }
            orders.addLast(new ChildOrderRef().set(childOrder.childOrderId(), childOrder.parentOrderId(),
                    childOrder.venueId(), childOrder.side(), childOrder.quantity(), childOrder.limitPrice(),
                    childOrder.createdEpochNanos()));
            return true;
        }

        synchronized ChildOrderRef poll() {
            return orders.pollFirst();
        }
    }
}
