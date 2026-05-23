package com.nitroj.sor.api.spi;

/**
 * Responsibility: engine-side callback for quote updates.
 *
 * <p>Role in system: implemented by the framework engine, not by normal
 * integrators. Market data sources invoke it when top-of-book data changes.</p>
 *
 * <p>Relationships: receives mutable reusable {@link Quote} carriers.</p>
 *
 * <p>Lifecycle: registered through {@link MarketDataSource#subscribe} and
 * removed through unsubscribe.</p>
 *
 * <p>Design intent: make quote delivery allocation-free after warmup.</p>
 */
@FunctionalInterface
public interface MarketDataListener {
    /**
     * Receives a quote update. The listener must copy any fields it needs after
     * the callback returns because the source may reuse the same instance.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Must return within
     * 1 microsecond for normal top-of-book updates.</p>
     *
     * @param quote reusable quote carrier
     */
    void onQuote(Quote quote);
}
