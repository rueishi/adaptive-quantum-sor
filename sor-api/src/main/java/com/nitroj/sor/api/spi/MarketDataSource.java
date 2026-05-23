package com.nitroj.sor.api.spi;

/**
 * Responsibility: integrator-supplied source of market quotes.
 *
 * <p>Role in system: the engine subscribes to instruments through this
 * interface; real feeds and simulators implement it.</p>
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
     * Subscribes a listener for an instrument.
     *
     * <p>Control-plane method, not hot-path. Implementations may allocate while
     * changing subscription state.</p>
     *
     * @param instrumentId instrument to subscribe
     * @param listener engine-owned listener
     */
    void subscribe(int instrumentId, MarketDataListener listener);

    /**
     * Removes a listener subscription for an instrument.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param instrumentId instrument to unsubscribe
     * @param listener engine-owned listener
     */
    void unsubscribe(int instrumentId, MarketDataListener listener);
}
