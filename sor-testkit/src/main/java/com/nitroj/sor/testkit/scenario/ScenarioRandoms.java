package com.nitroj.sor.testkit.scenario;

import java.util.SplittableRandom;

/**
 * Responsibility: own independent deterministic random streams for scenario
 * domains.
 *
 * <p>Role in system: scenario replay must not change market data merely
 * because order flow or venue outcomes consumed an extra random number. This
 * class derives separate streams from one scenario seed.</p>
 *
 * <p>Relationships: used by {@link ScenarioRunner} and later low-level
 * simulator overloads.</p>
 *
 * <p>Lifecycle: created at scenario start and consumed during one run.</p>
 *
 * <p>Design intent: named streams make deterministic replay robust while still
 * allowing each domain to be stochastic.</p>
 */
public final class ScenarioRandoms {
    private static final long MARKET_SALT = 0x4d41524b45544cL;
    private static final long ORDER_SALT = 0x0f0d0e0c0b0a0908L;
    private static final long VENUE_SALT = 0x56454e55454cL;
    private static final long SESSION_SALT = 0x5153455353494f4eL;

    private final SplittableRandom market;
    private final SplittableRandom order;
    private final SplittableRandom venue;
    private final SplittableRandom session;

    public ScenarioRandoms(final long seed) {
        this.market = new SplittableRandom(seed ^ MARKET_SALT);
        this.order = new SplittableRandom(seed ^ ORDER_SALT);
        this.venue = new SplittableRandom(seed ^ VENUE_SALT);
        this.session = new SplittableRandom(seed ^ SESSION_SALT);
    }

    public int nextMarketInt(final int bound) {
        return market.nextInt(bound);
    }

    public int nextOrderInt(final int bound) {
        return order.nextInt(bound);
    }

    public int nextVenueInt(final int bound) {
        return venue.nextInt(bound);
    }

    public int nextSessionInt(final int bound) {
        return session.nextInt(bound);
    }
}
