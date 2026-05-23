package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.MarketDataListener;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Quote;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Responsibility: deterministic SPI market data feed and market-session source.
 *
 * <p>Role in system: publishes reusable {@link Quote} carriers to the engine
 * while retaining simulator-local current book, feed-health, and session state
 * for scenario replay.</p>
 *
 * <p>Relationships: consumed by {@link SimulatedClusterController}, core
 * scenario orchestration, and tests through the {@link MarketDataSource}
 * listener contract.</p>
 *
 * <p>Lifecycle: constructed with deterministic config/seed, subscribed by the
 * engine, advanced by explicit {@link #generateTick()} calls, then closed by
 * the owning runner.</p>
 *
 * <p>Design intent: reproduce legacy seeded mid evolution, profile spreads,
 * displayed quantity mean reversion, stale-feed windows, and sanitized external
 * updates without writing core internal state.</p>
 */
public final class SimulatedMarketDataSource implements MarketDataSource {
    private final Map<Integer, CopyOnWriteArrayList<MarketDataListener>> listeners = new ConcurrentHashMap<>();
    private final Quote reusableQuote = new Quote();
    private final SimConfig config;
    private final Random random;
    private final long[] midByInstrument;
    private final long[] bidQtyByInstrumentVenue;
    private final long[] askQtyByInstrumentVenue;
    private final long[] lastBidByInstrumentVenue;
    private final long[] lastAskByInstrumentVenue;
    private final boolean[] staleByVenue;
    private final SimMarketBookSnapshot book;
    private final SimFeedHealthSnapshot feedHealth;
    private long tick;

    public SimulatedMarketDataSource() {
        this(SimConfig.defaults(), 0L);
    }

    public SimulatedMarketDataSource(final SimConfig config, final long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.random = new Random(seed);
        this.midByInstrument = new long[config.instrumentCount()];
        final int cellCount = config.instrumentCount() * config.venueCount();
        this.bidQtyByInstrumentVenue = new long[cellCount];
        this.askQtyByInstrumentVenue = new long[cellCount];
        this.lastBidByInstrumentVenue = new long[cellCount];
        this.lastAskByInstrumentVenue = new long[cellCount];
        this.staleByVenue = new boolean[config.venueCount()];
        this.book = new SimMarketBookSnapshot(config);
        this.feedHealth = new SimFeedHealthSnapshot(config.venueCount());
        initializeState();
    }

    @Override
    public void subscribe(final int instrumentId, final MarketDataListener listener) {
        listeners.computeIfAbsent(instrumentId, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unsubscribe(final int instrumentId, final MarketDataListener listener) {
        final List<MarketDataListener> bucket = listeners.get(instrumentId);
        if (bucket != null) {
            bucket.remove(listener);
        }
    }

    public void publish(final int instrumentId, final long bidPrice, final long askPrice,
                        final long bidQuantity, final long askQuantity, final long epochNanos) {
        final List<MarketDataListener> bucket = listeners.get(instrumentId);
        reusableQuote.set(instrumentId, bidPrice, askPrice, bidQuantity, askQuantity, epochNanos);
        if (instrumentId >= 0 && instrumentId < config.instrumentCount()) {
            book.updateTopOfBook(instrumentId, 0, bidPrice, askPrice, bidQuantity, askQuantity);
        }
        if (bucket != null) {
            bucket.forEach(listener -> listener.onQuote(reusableQuote));
        }
    }

    /** Marks every instrument open in a simulator-local market-session snapshot. */
    public SimMarketSessionSnapshot openAllSessions() {
        final SimMarketSessionSnapshot snapshot = new SimMarketSessionSnapshot(config.instrumentCount());
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            snapshot.setOpen(instrumentId, true);
        }
        return snapshot;
    }

    /** Applies legacy thin-book halt windows to market session state. */
    public void applyMarketSession(final SimMarketSessionSnapshot snapshot, final int regimeId, final int scenarioTick) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            final boolean haltedForAuction = regimeId == SimRegime.THIN_BOOK
                    && instrumentId == Math.floorMod(scenarioTick, config.instrumentCount())
                    && scenarioTick % 5 == 0;
            snapshot.setOpen(instrumentId, !haltedForAuction);
        }
    }

    /** Generates one legacy-equivalent tick using stable venue profiles. */
    public void generateTick() {
        generateTick(SimRegime.NORMAL);
    }

    /** Generates one tick for a single active regime across all instruments. */
    public void generateTick(final int regimeId) {
        generateTick(regimeId, tick);
    }

    /** Generates one tick for a scenario tick and publishes through listeners. */
    public void generateTick(final int regimeId, final long scenarioTick) {
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final SimVenueProfile profile = SimVenueProfile.forVenue(venueId);
            staleByVenue[venueId] = isStale(profile);
            feedHealth.update(venueId, tick + 1L, staleByVenue[venueId]);
        }
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            final int activeRegime = Math.min(regimeId, Math.max(0, config.regimeCount() - 1));
            midByInstrument[instrumentId] = Math.max(1L, midByInstrument[instrumentId] + shock(activeRegime));
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final int idx = config.cellIndex(instrumentId, venueId);
                final SimVenueProfile profile = SimVenueProfile.forVenue(venueId);
                if (!staleByVenue[venueId]) {
                    final long spread = spread(activeRegime, profile);
                    final long venueNoise = random.nextInt(3) - 1L;
                    final long venueMid = Math.max(1L, midByInstrument[instrumentId] + venueNoise);
                    lastBidByInstrumentVenue[idx] = Math.max(1L, venueMid - spread);
                    lastAskByInstrumentVenue[idx] = Math.max(lastBidByInstrumentVenue[idx] + 1L, venueMid + spread);
                    bidQtyByInstrumentVenue[idx] = nextQty(bidQtyByInstrumentVenue[idx], targetQty(activeRegime, profile));
                    askQtyByInstrumentVenue[idx] = nextQty(askQtyByInstrumentVenue[idx], targetQty(activeRegime, profile));
                }
                book.updateTopOfBook(instrumentId, venueId, lastBidByInstrumentVenue[idx],
                        lastAskByInstrumentVenue[idx], bidQtyByInstrumentVenue[idx], askQtyByInstrumentVenue[idx]);
                final List<MarketDataListener> bucket = listeners.get(instrumentId);
                if (bucket != null) {
                    reusableQuote.set(instrumentId, lastBidByInstrumentVenue[idx], lastAskByInstrumentVenue[idx],
                            bidQtyByInstrumentVenue[idx], askQtyByInstrumentVenue[idx], scenarioTick);
                    bucket.forEach(listener -> listener.onQuote(reusableQuote));
                }
            }
        }
        tick++;
    }

    /** Applies one externally supplied book update after sanitizing invalid values. */
    public void applySanitized(
            final int instrumentId,
            final int venueId,
            final long rawBid,
            final long rawAsk,
            final long rawBidQty,
            final long rawAskQty
    ) {
        final long bid = Math.max(1L, rawBid);
        final long ask = Math.max(bid + 1L, rawAsk);
        final int idx = config.cellIndex(instrumentId, venueId);
        lastBidByInstrumentVenue[idx] = bid;
        lastAskByInstrumentVenue[idx] = ask;
        bidQtyByInstrumentVenue[idx] = Math.max(0L, rawBidQty);
        askQtyByInstrumentVenue[idx] = Math.max(0L, rawAskQty);
        book.updateTopOfBook(instrumentId, venueId, bid, ask, bidQtyByInstrumentVenue[idx], askQtyByInstrumentVenue[idx]);
    }

    public SimMarketBookSnapshot currentBook() {
        return book;
    }

    public SimFeedHealthSnapshot feedHealth() {
        return feedHealth;
    }

    public long currentMidTicks(final int instrumentId) {
        config.checkInstrument(instrumentId);
        return midByInstrument[instrumentId];
    }

    public long currentBidQty(final int instrumentId, final int venueId) {
        return bidQtyByInstrumentVenue[config.cellIndex(instrumentId, venueId)];
    }

    public long currentAskQty(final int instrumentId, final int venueId) {
        return askQtyByInstrumentVenue[config.cellIndex(instrumentId, venueId)];
    }

    public long tick() {
        return tick;
    }

    private void initializeState() {
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            midByInstrument[instrumentId] = 10_000L + instrumentId * 100L;
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final int idx = config.cellIndex(instrumentId, venueId);
                lastBidByInstrumentVenue[idx] = Math.max(1L, midByInstrument[instrumentId] - 1L);
                lastAskByInstrumentVenue[idx] = midByInstrument[instrumentId] + 1L;
                bidQtyByInstrumentVenue[idx] = 5_000L;
                askQtyByInstrumentVenue[idx] = 5_000L;
                book.updateTopOfBook(instrumentId, venueId, lastBidByInstrumentVenue[idx],
                        lastAskByInstrumentVenue[idx], bidQtyByInstrumentVenue[idx], askQtyByInstrumentVenue[idx]);
            }
        }
    }

    private long shock(final int regimeId) {
        final int bound;
        if (regimeId == SimRegime.VOLATILE) {
            bound = 8;
        } else if (regimeId == SimRegime.THIN_BOOK) {
            bound = 3;
        } else {
            bound = 1;
        }
        return random.nextInt(bound * 2 + 1) - bound;
    }

    private long spread(final int regimeId, final SimVenueProfile profile) {
        final long regimeSpread = regimeId == SimRegime.VOLATILE ? 4L : regimeId == SimRegime.THIN_BOOK ? 3L : 2L;
        return Math.max(1L, regimeSpread * profile.spreadMultiplier);
    }

    private long targetQty(final int regimeId, final SimVenueProfile profile) {
        if (regimeId == SimRegime.THIN_BOOK) {
            return Math.min(profile.targetQty, 500L);
        }
        if (regimeId == SimRegime.VOLATILE) {
            return Math.max(1_000L, profile.targetQty / 2L);
        }
        return profile.targetQty;
    }

    private long nextQty(final long previousQty, final long targetQty) {
        final long meanReversion = (targetQty - previousQty) / 4L;
        final long noise = random.nextInt(401) - 200L;
        return Math.max(0L, previousQty + meanReversion + noise);
    }

    private boolean isStale(final SimVenueProfile profile) {
        return profile.staleProbabilityBps > 0 && random.nextInt(10_000) < profile.staleProbabilityBps;
    }
}
