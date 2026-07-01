package com.nitroj.sor.api;

/**
 * Responsibility: immutable control-plane summary of engine-owned market data.
 *
 * <p>Role in system: exposes sequence, checksum, population count, and last
 * update time without returning mutable market-book arrays.</p>
 *
 * <p>Relationships: returned by
 * {@link SorControlPlane#marketDataSnapshot()}.</p>
 *
 * <p>Lifecycle: produced on demand and owned by the caller.</p>
 *
 * <p>Design intent: let scenario replay and diagnostics compare market state
 * safely.</p>
 *
 * @param sequence market-data update sequence
 * @param checksum deterministic checksum over populated market data
 * @param populatedCellCount number of populated instrument/venue cells
 * @param lastUpdateEpochNanos wall-clock timestamp of the last update
 */
public record MarketDataSnapshotSummary(
        long sequence,
        long checksum,
        int populatedCellCount,
        long lastUpdateEpochNanos
) {
    /**
     * Validates non-negative summary fields.
     */
    public MarketDataSnapshotSummary {
        if (sequence < 0 || populatedCellCount < 0 || lastUpdateEpochNanos < 0) {
            throw new IllegalArgumentException("market data summary values must be non-negative");
        }
    }
}
