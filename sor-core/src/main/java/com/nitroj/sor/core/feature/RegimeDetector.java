package com.nitroj.sor.core.feature;

import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.stats.RegimeState;

/**
 * Responsibility: classify simple Phase 1 market regimes from top-of-book data.
 *
 * <p>Role in system: regime detection provides the route-key regime dimension
 * used by policy SOR and optimizer snapshots.</p>
 *
 * <p>Relationships: {@link FeatureAggregator} calls this detector after stats
 * aggregation. It reads {@link MarketBookState} and writes {@link RegimeState}.</p>
 *
 * <p>Lifecycle: stateless service object reused across aggregation passes.</p>
 *
 * <p>Design intent: Phase 1 deliberately supports only NORMAL, VOLATILE, and
 * THIN_BOOK using deterministic spread and liquidity thresholds.</p>
 */
public final class RegimeDetector {
    private final RollingWindowConfig config;

    public RegimeDetector(final RollingWindowConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    /**
     * Detects one instrument regime from all venues in the market book.
     */
    public int detect(final MarketBookState marketBookState, final int instrumentId, final int venueCount) {
        if (marketBookState == null) {
            throw new IllegalArgumentException("marketBookState must not be null");
        }
        long totalVisibleQty = 0L;
        long maxSpread = 0L;
        for (int venueId = 0; venueId < venueCount; venueId++) {
            final long bid = marketBookState.bidPriceTicks(instrumentId, venueId);
            final long ask = marketBookState.askPriceTicks(instrumentId, venueId);
            if (bid > 0 && ask > bid) {
                maxSpread = Math.max(maxSpread, ask - bid);
            }
            totalVisibleQty += marketBookState.bidQty(instrumentId, venueId);
            totalVisibleQty += marketBookState.askQty(instrumentId, venueId);
        }
        if (totalVisibleQty <= config.thinBookQuantityThreshold) {
            return RegimeState.THIN_BOOK;
        }
        if (maxSpread >= config.volatileSpreadThresholdTicks && config.volatileSpreadThresholdTicks > 0) {
            return RegimeState.VOLATILE;
        }
        return RegimeState.NORMAL;
    }
}
