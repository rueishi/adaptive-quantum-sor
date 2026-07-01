package com.nitroj.sor.core.optimizer.batch;

/**
 * Responsibility: identify one active parent order in a batch allocation run.
 *
 * <p>Role in system: Phase 6 batch allocation optimizes venue usage across
 * multiple parent orders, so each parent needs stable identity, instrument,
 * side, and remaining quantity in the optimizer snapshot.</p>
 */
public record BatchParentOrderSnapshot(
        long parentOrderId,
        int instrumentId,
        int side,
        long quantity
) {
    public BatchParentOrderSnapshot {
        if (parentOrderId <= 0L) {
            throw new IllegalArgumentException("parentOrderId must be positive");
        }
        if (instrumentId < 0) {
            throw new IllegalArgumentException("instrumentId must be non-negative");
        }
        if (side != 1 && side != 2) {
            throw new IllegalArgumentException("side must be BUY(1) or SELL(2)");
        }
        if (quantity <= 0L) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
