package com.nitroj.sor.api;

/**
 * Responsibility: immutable venue-aware top-of-book cell for startup market
 * hydration.
 *
 * <p>Role in system: lets the market-data plant or testkit provide the initial
 * routing book without exposing mutable engine market-book arrays.</p>
 *
 * <p>Relationships: contained by {@link MarketDataSeedSnapshot} and maps to one
 * instrument/venue cell in the engine-owned market book.</p>
 *
 * <p>Lifecycle: created for one startup snapshot and discarded after the engine
 * copies primitive fields into its own state.</p>
 *
 * <p>Design intent: make initial market state explicit and venue-scoped.</p>
 *
 * @param instrumentId dense instrument identifier
 * @param venueId dense venue identifier
 * @param bidPrice best bid price in fixed-point units
 * @param askPrice best ask price in fixed-point units
 * @param bidQuantity displayed bid quantity
 * @param askQuantity displayed ask quantity
 * @param epochNanos source quote timestamp
 */
public record MarketDataSeedCell(
        int instrumentId,
        int venueId,
        long bidPrice,
        long askPrice,
        long bidQuantity,
        long askQuantity,
        long epochNanos
) {
    /**
     * Validates top-of-book seed values before startup hydration.
     */
    public MarketDataSeedCell {
        if (instrumentId < 0 || venueId < 0 || bidPrice <= 0 || askPrice <= 0
                || bidQuantity < 0 || askQuantity < 0 || epochNanos < 0) {
            throw new IllegalArgumentException("market data seed cell inputs must be valid");
        }
        if (bidPrice >= askPrice) {
            throw new IllegalArgumentException("market data seed bid must be less than ask");
        }
    }
}
