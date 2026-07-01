package com.nitroj.sor.api.spi;

/**
 * Responsibility: integrator-supplied source of market quotes.
 *
 * <p>Role in system: the engine subscribes to instrument/venue books through
 * this interface; real feeds and simulators implement it. Instrument-only
 * subscriptions are retained as a documented wildcard for migration and
 * broadcast use.</p>
 *
 * <p>Relationships: delivers reusable {@link Quote} objects to
 * {@link MarketDataListener}.</p>
 *
 * <p>Lifecycle: created before engine build, subscribed during startup, and
 * unsubscribed during shutdown.</p>
 *
 * <p>Design intent: keep market-data ownership outside the engine.</p>
 */
public interface MarketDataSource {
    /**
     * Subscribes a listener for a single instrument and venue.
     *
     * <p>Control-plane method, not hot-path. Implementations may allocate while
     * changing subscription state. The default delegates to the legacy
     * instrument-only wildcard subscription.</p>
     *
     * @param instrumentId instrument to subscribe
     * @param venueId venue to subscribe
     * @param listener engine-owned listener
     */
    default void subscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
        subscribe(instrumentId, listener);
    }

    /**
     * Subscribes a listener for an instrument.
     *
     * <p>Control-plane method, not hot-path. Implementations may allocate while
     * changing subscription state. This instrument-only form is a wildcard that
     * may receive quotes from every venue for the instrument.</p>
     *
     * @param instrumentId instrument to subscribe
     * @param listener engine-owned listener
     */
    void subscribe(int instrumentId, MarketDataListener listener);

    /**
     * Removes a listener subscription for a single instrument and venue.
     *
     * <p>Control-plane method, not hot-path. The default delegates to the
     * legacy instrument-only wildcard unsubscription.</p>
     *
     * @param instrumentId instrument to unsubscribe
     * @param venueId venue to unsubscribe
     * @param listener engine-owned listener
     */
    default void unsubscribe(final int instrumentId, final int venueId, final MarketDataListener listener) {
        unsubscribe(instrumentId, listener);
    }

    /**
     * Removes a listener subscription for an instrument.
     *
     * <p>Control-plane method, not hot-path. This removes the instrument-only
     * wildcard subscription.</p>
     *
     * @param instrumentId instrument to unsubscribe
     * @param listener engine-owned listener
     */
    void unsubscribe(int instrumentId, MarketDataListener listener);
}
