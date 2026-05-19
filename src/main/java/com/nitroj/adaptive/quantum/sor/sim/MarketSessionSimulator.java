package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

/**
 * Responsibility: generate deterministic market session state.
 *
 * <p>Role in system: simulates whether each instrument can trade during the
 * current scenario.</p>
 *
 * <p>Relationships: writes {@link MarketSessionState}; regime and execution
 * layers read it as a market-open guard.</p>
 *
 * <p>Lifecycle: run at scenario setup or simulated open/close transitions.</p>
 *
 * <p>Design intent: Phase 1 uses a boolean per instrument instead of real
 * calendars or exchange schedules.</p>
 */
public final class MarketSessionSimulator {
    private final SorConfig config;

    public MarketSessionSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /** Marks all instruments open. */
    public void openAll(final MarketSessionState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            state.setOpen(instrumentId, true);
        }
    }

    /**
     * Applies deterministic scenario market-session windows.
     *
     * <p>Core logic: all instruments are open during normal/volatile windows;
     * thin-book windows close one rotating instrument every fifth tick to model
     * halt/auction-like non-executable state without adding full exchange
     * calendars. The caller supplies the isolated {@link ScenarioState} and the
     * method mutates only the provided {@link MarketSessionState}.</p>
     *
     * @param state market-session state to update
     * @param scenarioState current scenario context and simulated clock
     * @throws IllegalArgumentException when inputs are null
     */
    public void applyScenario(final MarketSessionState state, final ScenarioState scenarioState) {
        if (state == null || scenarioState == null) {
            throw new IllegalArgumentException("state and scenarioState must not be null");
        }
        final int tick = scenarioState.clock().tick();
        final int regimeId = scenarioState.spec().regimeAt(tick);
        for (int instrumentId = 0; instrumentId < config.instrumentCount(); instrumentId++) {
            final boolean haltedForAuction = regimeId == RegimeState.THIN_BOOK
                    && instrumentId == tick % config.instrumentCount()
                    && tick % 5 == 0;
            state.setOpen(instrumentId, !haltedForAuction);
        }
    }
}
