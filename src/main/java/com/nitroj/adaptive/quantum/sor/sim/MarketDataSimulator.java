package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioVenueProfile;
import com.nitroj.adaptive.quantum.sor.state.FeedHealthState;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

import java.util.Random;

/**
 * Responsibility: generate deterministic top-of-book market data.
 *
 * <p>Role in system: simulates exchange L1 updates for every configured
 * instrument/venue pair.</p>
 *
 * <p>Relationships: writes {@link MarketBookState}; feature aggregation and
 * execution read the resulting valid bid/ask prices and quantities.</p>
 *
 * <p>Lifecycle: created with config and seed and called per synthetic tick.</p>
 *
 * <p>Design intent: invalid generated prices are corrected before publication
 * so downstream code never sees crossed or negative books from this simulator.</p>
 */
public final class MarketDataSimulator {
    private final SorConfig config;
    private final Random random;
    private final long[] midByInstrument;
    private final long[] bidQtyByInstrumentVenue;
    private final long[] askQtyByInstrumentVenue;
    private final long[] lastBidByInstrumentVenue;
    private final long[] lastAskByInstrumentVenue;
    private final boolean[] staleByVenue;
    private long tick;

    public MarketDataSimulator(final SorConfig config, final long seed) {
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
        initializeState();
    }

    /** Updates every top-of-book cell once with valid bounded values. */
    public void generateTick(final MarketBookState state) {
        generateTick(state, (RegimeState) null);
    }

    /**
     * Updates every top-of-book cell using persistent state and one active
     * regime per instrument.
     *
     * <p>Core logic: instrument mids evolve by bounded regime-specific shocks,
     * venue quotes are derived from the shared instrument mid, and displayed
     * quantities mean-revert toward regime/profile targets. Published books are
     * always sanitized through {@link MarketBookState#updateTopOfBook(int, int, long, long, long, long)}.</p>
     */
    public void generateTick(final MarketBookState state, final RegimeState regimes) {
        generateTick(state, regimes, null, null);
    }

    /**
     * Updates books for a scenario-driven run and optionally records feed
     * staleness. This method keeps orchestration outside the simulator: it only
     * reads the current scenario tick/profile data supplied by
     * {@link ScenarioState}.
     */
    public void generateTick(final MarketBookState state, final ScenarioState scenarioState) {
        generateTick(state, null, scenarioState, null);
    }

    /**
     * Updates books for a scenario-driven run and records feed health when a
     * stale-feed profile triggers.
     */
    public void generateTick(
            final MarketBookState state,
            final ScenarioState scenarioState,
            final FeedHealthState feedHealthState
    ) {
        generateTick(state, null, scenarioState, feedHealthState);
    }

    private void generateTick(
            final MarketBookState state,
            final RegimeState regimes,
            final ScenarioState scenarioState,
            final FeedHealthState feedHealthState
    ) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int venueId = 0; venueId < config.venueCount(); venueId++) {
            final ScenarioVenueProfile profile = scenarioState == null
                    ? ScenarioVenueProfile.forVenue(venueId)
                    : scenarioState.profile(venueId);
            staleByVenue[venueId] = isStale(profile);
            if (feedHealthState != null) {
                feedHealthState.update(venueId, tick + 1L, staleByVenue[venueId]);
            }
        }
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            final int regimeId = activeRegime(instrumentId, regimes, scenarioState);
            midByInstrument[instrumentId] = Math.max(1L, midByInstrument[instrumentId] + shock(regimeId));
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final int idx = idx(instrumentId, venueId);
                final ScenarioVenueProfile profile = scenarioState == null
                        ? ScenarioVenueProfile.forVenue(venueId)
                        : scenarioState.profile(venueId);
                if (!staleByVenue[venueId]) {
                    final long spread = spread(regimeId, profile);
                    final long venueNoise = random.nextInt(3) - 1L;
                    final long venueMid = Math.max(1L, midByInstrument[instrumentId] + venueNoise);
                    lastBidByInstrumentVenue[idx] = Math.max(1L, venueMid - spread);
                    lastAskByInstrumentVenue[idx] = Math.max(lastBidByInstrumentVenue[idx] + 1L, venueMid + spread);
                    bidQtyByInstrumentVenue[idx] = nextQty(bidQtyByInstrumentVenue[idx], targetQty(regimeId, profile));
                    askQtyByInstrumentVenue[idx] = nextQty(askQtyByInstrumentVenue[idx], targetQty(regimeId, profile));
                }
                state.updateTopOfBook(
                        instrumentId,
                        venueId,
                        lastBidByInstrumentVenue[idx],
                        lastAskByInstrumentVenue[idx],
                        bidQtyByInstrumentVenue[idx],
                        askQtyByInstrumentVenue[idx]
                );
            }
        }
        tick++;
    }

    /**
     * Applies one externally supplied book update after correcting invalid
     * values into a valid non-crossed book.
     */
    public void applySanitized(
            final MarketBookState state,
            final int instrumentId,
            final int venueId,
            final long rawBid,
            final long rawAsk,
            final long rawBidQty,
            final long rawAskQty
    ) {
        final long bid = Math.max(1L, rawBid);
        final long ask = Math.max(bid + 1L, rawAsk);
        state.updateTopOfBook(instrumentId, venueId, bid, ask, Math.max(0L, rawBidQty), Math.max(0L, rawAskQty));
    }

    public long currentMidTicks(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= midByInstrument.length) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
        return midByInstrument[instrumentId];
    }

    public long currentBidQty(final int instrumentId, final int venueId) {
        return bidQtyByInstrumentVenue[idx(instrumentId, venueId)];
    }

    public long currentAskQty(final int instrumentId, final int venueId) {
        return askQtyByInstrumentVenue[idx(instrumentId, venueId)];
    }

    public long tick() {
        return tick;
    }

    private void initializeState() {
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            midByInstrument[instrumentId] = 10_000L + instrumentId * 100L;
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                final int idx = idx(instrumentId, venueId);
                lastBidByInstrumentVenue[idx] = Math.max(1L, midByInstrument[instrumentId] - 1L);
                lastAskByInstrumentVenue[idx] = midByInstrument[instrumentId] + 1L;
                bidQtyByInstrumentVenue[idx] = 5_000L;
                askQtyByInstrumentVenue[idx] = 5_000L;
            }
        }
    }

    private int activeRegime(
            final int instrumentId,
            final RegimeState regimes,
            final ScenarioState scenarioState
    ) {
        if (scenarioState != null) {
            return Math.min(scenarioState.spec().regimeAt(scenarioState.clock().tick()), Math.max(0, config.regimeCount() - 1));
        }
        if (regimes != null) {
            return Math.min(regimes.regime(instrumentId), Math.max(0, config.regimeCount() - 1));
        }
        return RegimeState.NORMAL;
    }

    private long shock(final int regimeId) {
        final int bound;
        if (regimeId == RegimeState.VOLATILE) {
            bound = 8;
        } else if (regimeId == RegimeState.THIN_BOOK) {
            bound = 3;
        } else {
            bound = 1;
        }
        return random.nextInt(bound * 2 + 1) - bound;
    }

    private long spread(final int regimeId, final ScenarioVenueProfile profile) {
        final long regimeSpread = regimeId == RegimeState.VOLATILE ? 4L : regimeId == RegimeState.THIN_BOOK ? 3L : 2L;
        return Math.max(1L, regimeSpread * profile.spreadMultiplier);
    }

    private long targetQty(final int regimeId, final ScenarioVenueProfile profile) {
        if (regimeId == RegimeState.THIN_BOOK) {
            return Math.min(profile.targetQty, 500L);
        }
        if (regimeId == RegimeState.VOLATILE) {
            return Math.max(1_000L, profile.targetQty / 2L);
        }
        return profile.targetQty;
    }

    private long nextQty(final long previousQty, final long targetQty) {
        final long meanReversion = (targetQty - previousQty) / 4L;
        final long noise = random.nextInt(401) - 200L;
        return Math.max(0L, previousQty + meanReversion + noise);
    }

    private boolean isStale(final ScenarioVenueProfile profile) {
        return profile.staleProbabilityBps > 0 && random.nextInt(10_000) < profile.staleProbabilityBps;
    }

    private int idx(final int instrumentId, final int venueId) {
        if (instrumentId < 0 || instrumentId >= config.instrumentCount()) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
        if (venueId < 0 || venueId >= config.venueCount()) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
        return instrumentId * config.venueCount() + venueId;
    }
}
