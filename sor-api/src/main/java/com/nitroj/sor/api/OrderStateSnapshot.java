package com.nitroj.sor.api;

import java.util.HashSet;

/**
 * Responsibility: immutable OMS/EMS order-state snapshot for startup
 * hydration.
 *
 * <p>Role in system: carries both parent and child records so the engine can
 * rebuild working state atomically without replaying recovered orders through
 * hot-path submission or child-emission methods.</p>
 *
 * <p>Relationships: supplied by {@link SorStartupStateSource}, consumed by
 * {@link SorStartupHydrationRequest}, and later applied to engine-owned order
 * state by `sor-core`.</p>
 *
 * <p>Lifecycle: produced by OMS/EMS, recovery, or deterministic testkit code
 * for one as-of point and owned by the control-plane caller.</p>
 *
 * <p>Design intent: distinguish idempotent snapshot replay from corrupt
 * duplicate IDs inside a single snapshot.</p>
 *
 * @param snapshotId source snapshot identifier
 * @param asOfSequence source sequence watermark
 * @param asOfEpochNanos source as-of timestamp
 * @param parents active parent order records
 * @param children active child order records
 */
public record OrderStateSnapshot(
        String snapshotId,
        long asOfSequence,
        long asOfEpochNanos,
        ParentOrderStateSnapshot[] parents,
        ChildOrderStateSnapshot[] children
) {
    /**
     * Validates metadata, parent/child uniqueness, parent linkage, and
     * defensively copies caller arrays.
     */
    public OrderStateSnapshot {
        if (snapshotId == null || snapshotId.isBlank() || asOfSequence < 0
                || asOfEpochNanos < 0 || parents == null || children == null) {
            throw new IllegalArgumentException("order state snapshot inputs must be valid");
        }
        parents = parents.clone();
        children = children.clone();
        final HashSet<Long> parentIds = new HashSet<>();
        for (ParentOrderStateSnapshot parent : parents) {
            if (parent == null) {
                throw new IllegalArgumentException("order state parent snapshots must not contain null");
            }
            if (!parentIds.add(parent.parentOrderId())) {
                throw new IllegalArgumentException("order state snapshot contains duplicate parentOrderId");
            }
        }
        final HashSet<Long> childIds = new HashSet<>();
        for (ChildOrderStateSnapshot child : children) {
            if (child == null) {
                throw new IllegalArgumentException("order state child snapshots must not contain null");
            }
            if (!childIds.add(child.childOrderId())) {
                throw new IllegalArgumentException("order state snapshot contains duplicate childOrderId");
            }
            if (!parentIds.contains(child.parentOrderId())) {
                throw new IllegalArgumentException("child order snapshot references missing parentOrderId");
            }
        }
    }

    /**
     * Creates an auditable empty order-state snapshot for scopes with no active
     * OMS/EMS working orders.
     *
     * @param snapshotId source snapshot identifier
     * @param asOfSequence source sequence watermark
     * @param asOfEpochNanos source as-of timestamp
     * @return empty snapshot
     */
    public static OrderStateSnapshot empty(final String snapshotId, final long asOfSequence,
                                           final long asOfEpochNanos) {
        return new OrderStateSnapshot(snapshotId, asOfSequence, asOfEpochNanos,
                new ParentOrderStateSnapshot[0], new ChildOrderStateSnapshot[0]);
    }

    /**
     * Returns a defensive copy of parent records.
     *
     * @return parent records
     */
    @Override
    public ParentOrderStateSnapshot[] parents() {
        return parents.clone();
    }

    /**
     * Returns a defensive copy of child records.
     *
     * @return child records
     */
    @Override
    public ChildOrderStateSnapshot[] children() {
        return children.clone();
    }

    /**
     * Returns a stable checksum over source metadata and parent/child records.
     *
     * @return deterministic checksum
     */
    public long checksum() {
        long checksum = snapshotId.hashCode() * 31L + asOfSequence;
        checksum = checksum * 31L + asOfEpochNanos;
        for (ParentOrderStateSnapshot parent : parents) {
            checksum = checksum * 31L + parent.parentOrderId();
            checksum = checksum * 31L + parent.instrumentId();
            checksum = checksum * 31L + parent.side();
            checksum = checksum * 31L + parent.limitPrice();
            checksum = checksum * 31L + parent.originalQuantity();
            checksum = checksum * 31L + parent.leavesQuantity();
            checksum = checksum * 31L + parent.filledQuantity();
            checksum = checksum * 31L + parent.pendingChildQuantity();
            checksum = checksum * 31L + parent.status().ordinal();
            checksum = checksum * 31L + parent.updatedEpochNanos();
        }
        for (ChildOrderStateSnapshot child : children) {
            checksum = checksum * 31L + child.childOrderId();
            checksum = checksum * 31L + child.parentOrderId();
            checksum = checksum * 31L + child.instrumentId();
            checksum = checksum * 31L + child.venueId();
            checksum = checksum * 31L + child.side();
            checksum = checksum * 31L + child.limitPrice();
            checksum = checksum * 31L + child.originalQuantity();
            checksum = checksum * 31L + child.leavesQuantity();
            checksum = checksum * 31L + child.filledQuantity();
            checksum = checksum * 31L + child.status().ordinal();
            checksum = checksum * 31L + child.updatedEpochNanos();
        }
        return checksum;
    }
}
