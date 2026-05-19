package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.MarketSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;
import com.nitroj.adaptive.quantum.sor.state.VenueThrottleState;

/**
 * Responsibility: coordinate the basic deterministic scenario generator shell.
 *
 * <p>Role in system: wires the individual external-source simulators into one
 * simple scenario setup entry point for tests and later demos.</p>
 *
 * <p>Relationships: owns no state; delegates to market, venue session, throttle,
 * fee, risk, and metadata simulators that populate their task-owned structures.</p>
 *
 * <p>Lifecycle: created with a fixed config/seed and invoked for scenario setup.</p>
 *
 * <p>Design intent: this shell proves the simulator family can be composed
 * without expanding into a replay engine or real matching simulator.</p>
 */
public final class SyntheticScenarioGenerator {
    private final MarketDataSimulator marketDataSimulator;
    private final VenueSessionSimulator venueSessionSimulator;
    private final VenueThrottleSimulator venueThrottleSimulator;
    private final MarketSessionSimulator marketSessionSimulator;

    public SyntheticScenarioGenerator(final SorConfig config, final long seed) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.marketDataSimulator = new MarketDataSimulator(config, seed);
        this.venueSessionSimulator = new VenueSessionSimulator(config);
        this.venueThrottleSimulator = new VenueThrottleSimulator(config);
        this.marketSessionSimulator = new MarketSessionSimulator(config);
    }

    /** Populates baseline valid scenario state. */
    public void populateBaseline(
            final MarketBookState marketBookState,
            final VenueSessionState venueSessionState,
            final VenueThrottleState venueThrottleState,
            final MarketSessionState marketSessionState
    ) {
        marketDataSimulator.generateTick(marketBookState);
        venueSessionSimulator.openAll(venueSessionState);
        venueThrottleSimulator.populate(venueThrottleState);
        marketSessionSimulator.openAll(marketSessionState);
    }
}
