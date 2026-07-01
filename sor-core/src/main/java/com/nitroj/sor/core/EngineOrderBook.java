package com.nitroj.sor.core;

import com.nitroj.sor.api.OrderStatusCode;
import com.nitroj.sor.api.spi.FillReport;
import com.nitroj.sor.api.spi.RejectReport;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsibility: maintain the engine-owned working order book for active
 * parent and child orders.
 *
 * <p>Role in system: gives `SorEngineImpl` local routing state without
 * synchronous OMS/EMS reads. OMS/EMS remains the external book of record, while
 * this structure tracks hot-path working quantities and lifecycle state.</p>
 *
 * <p>Relationships: populated by parent-order submission, later child-order
 * emission, venue fill/reject callbacks, and recovery/scenario seeds.</p>
 *
 * <p>Lifecycle: allocated once per engine instance, updated while the engine is
 * open, and discarded on close or future reset.</p>
 *
 * <p>Design intent: keep Phase 9 order-state ownership explicit while avoiding
 * any venue-integration behavior owned by later cards.</p>
 */
final class EngineOrderBook {
    private final ConcurrentHashMap<Long, ParentState> parents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ChildState> children = new ConcurrentHashMap<>();

    /**
     * Creates the working state for a newly accepted parent order.
     *
     * <p>Hot-path method. Updates local state only; it does not call OMS/EMS or
     * venue adapters.</p>
     */
    ParentSnapshot acceptParent(final long parentOrderId, final long quantity, final long updatedEpochNanos) {
        if (parentOrderId <= 0 || quantity <= 0 || updatedEpochNanos < 0) {
            throw new IllegalArgumentException("parentOrderId, quantity, and updatedEpochNanos must be valid");
        }
        final ParentState state = new ParentState(parentOrderId, quantity, 0, quantity, 0,
                OrderStatusCode.ACCEPTED, updatedEpochNanos);
        parents.put(parentOrderId, state);
        return state.snapshot();
    }

    /**
     * Records a child order after route emission.
     *
     * <p>Hot-path method. Later Phase 9 venue integration calls this when the
     * engine offers a child order to a venue ring.</p>
     */
    ParentSnapshot recordChildOrder(final long childOrderId, final long parentOrderId,
                                    final int venueId, final long quantity, final long updatedEpochNanos) {
        if (childOrderId <= 0 || parentOrderId <= 0 || venueId < 0 || quantity <= 0 || updatedEpochNanos < 0) {
            throw new IllegalArgumentException("child order inputs must be valid");
        }
        final ParentState parent = requireParent(parentOrderId);
        children.put(childOrderId, new ChildState(childOrderId, parentOrderId, venueId, quantity, quantity, 0,
                OrderStatusCode.ROUTED));
        parent.pendingChildQuantity += quantity;
        parent.status = OrderStatusCode.ROUTED;
        parent.updatedEpochNanos = updatedEpochNanos;
        return parent.snapshot();
    }

    /**
     * Applies a venue fill report to child and parent working state.
     *
     * <p>Hot-path callback. Unknown parents are ignored because late reports can
     * arrive after future reset/recovery boundaries.</p>
     */
    ParentSnapshot applyFill(final FillReport report) {
        if (report == null) {
            throw new IllegalArgumentException("report must not be null");
        }
        final ParentState parent = parents.get(report.parentOrderId());
        if (parent == null) {
            return null;
        }
        final ChildState child = children.get(report.childOrderId());
        if (child != null) {
            final long applied = Math.min(child.remainingQuantity, report.filledQuantity());
            child.filledQuantity += applied;
            child.remainingQuantity = Math.max(0, child.remainingQuantity - applied);
            child.status = child.remainingQuantity == 0 ? OrderStatusCode.FILLED : OrderStatusCode.PARTIALLY_FILLED;
            parent.pendingChildQuantity = Math.max(0, parent.pendingChildQuantity - applied);
        }
        parent.filledQuantity = Math.min(parent.originalQuantity, parent.filledQuantity + report.filledQuantity());
        parent.remainingQuantity = Math.max(0, parent.originalQuantity - parent.filledQuantity);
        parent.status = parent.remainingQuantity == 0 ? OrderStatusCode.FILLED : OrderStatusCode.PARTIALLY_FILLED;
        parent.updatedEpochNanos = report.filledEpochNanos();
        return parent.snapshot();
    }

    /**
     * Applies a venue reject report to child and parent working state.
     *
     * <p>Hot-path callback. Unknown parents are ignored because late reports can
     * arrive after future reset/recovery boundaries.</p>
     */
    ParentSnapshot applyReject(final RejectReport report) {
        if (report == null) {
            throw new IllegalArgumentException("report must not be null");
        }
        final ParentState parent = parents.get(report.parentOrderId());
        if (parent == null) {
            return null;
        }
        final ChildState child = children.get(report.childOrderId());
        if (child != null) {
            parent.pendingChildQuantity = Math.max(0, parent.pendingChildQuantity - child.remainingQuantity);
            child.remainingQuantity = 0;
            child.status = OrderStatusCode.REJECTED;
        }
        parent.status = OrderStatusCode.REJECTED;
        parent.updatedEpochNanos = report.rejectedEpochNanos();
        return parent.snapshot();
    }

    /**
     * Seeds parent working state from a recovery or scenario snapshot.
     *
     * <p>Control-plane method, not hot-path. This avoids pretending recovered
     * state arrived through new order submission.</p>
     */
    ParentSnapshot seedParent(final ParentSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        parents.put(snapshot.parentOrderId(), new ParentState(
                snapshot.parentOrderId(),
                snapshot.originalQuantity(),
                snapshot.filledQuantity(),
                snapshot.remainingQuantity(),
                snapshot.pendingChildQuantity(),
                snapshot.status(),
                snapshot.updatedEpochNanos()));
        return snapshot;
    }

    ParentSnapshot parent(final long parentOrderId) {
        final ParentState state = parents.get(parentOrderId);
        return state == null ? null : state.snapshot();
    }

    ChildSnapshot child(final long childOrderId) {
        final ChildState state = children.get(childOrderId);
        return state == null ? null : state.snapshot();
    }

    /**
     * Returns whether the working book contains active parent or child state.
     *
     * <p>Control-plane method, not hot-path.</p>
     */
    boolean hasLiveOrders() {
        return !parents.isEmpty() || !children.isEmpty();
    }

    int parentCount() {
        return parents.size();
    }

    int childCount() {
        return children.size();
    }

    long pendingChildQuantity() {
        return parents.values().stream().mapToLong(parent -> parent.pendingChildQuantity).sum();
    }

    /**
     * Clears all working parent and child order state.
     *
     * <p>Control-plane method, not hot-path. Used only by explicit reset or
     * recovery flows.</p>
     */
    void clear() {
        parents.clear();
        children.clear();
    }

    private ParentState requireParent(final long parentOrderId) {
        final ParentState parent = parents.get(parentOrderId);
        if (parent == null) {
            throw new IllegalArgumentException("unknown parentOrderId: " + parentOrderId);
        }
        return parent;
    }

    record ParentSnapshot(
            long parentOrderId,
            long originalQuantity,
            long filledQuantity,
            long remainingQuantity,
            long pendingChildQuantity,
            OrderStatusCode status,
            long updatedEpochNanos
    ) {
        ParentSnapshot {
            if (parentOrderId <= 0 || originalQuantity < 0 || filledQuantity < 0
                    || remainingQuantity < 0 || pendingChildQuantity < 0 || status == null
                    || updatedEpochNanos < 0) {
                throw new IllegalArgumentException("parent snapshot inputs must be valid");
            }
        }
    }

    record ChildSnapshot(
            long childOrderId,
            long parentOrderId,
            int venueId,
            long originalQuantity,
            long remainingQuantity,
            long filledQuantity,
            OrderStatusCode status
    ) {
        ChildSnapshot {
            if (childOrderId <= 0 || parentOrderId <= 0 || venueId < 0 || originalQuantity <= 0
                    || remainingQuantity < 0 || filledQuantity < 0 || status == null) {
                throw new IllegalArgumentException("child snapshot inputs must be valid");
            }
        }
    }

    private static final class ParentState {
        private final long parentOrderId;
        private final long originalQuantity;
        private long filledQuantity;
        private long remainingQuantity;
        private long pendingChildQuantity;
        private OrderStatusCode status;
        private long updatedEpochNanos;

        private ParentState(final long parentOrderId, final long originalQuantity, final long filledQuantity,
                            final long remainingQuantity, final long pendingChildQuantity,
                            final OrderStatusCode status, final long updatedEpochNanos) {
            this.parentOrderId = parentOrderId;
            this.originalQuantity = originalQuantity;
            this.filledQuantity = filledQuantity;
            this.remainingQuantity = remainingQuantity;
            this.pendingChildQuantity = pendingChildQuantity;
            this.status = status;
            this.updatedEpochNanos = updatedEpochNanos;
        }

        private ParentSnapshot snapshot() {
            return new ParentSnapshot(parentOrderId, originalQuantity, filledQuantity, remainingQuantity,
                    pendingChildQuantity, status, updatedEpochNanos);
        }
    }

    private static final class ChildState {
        private final long childOrderId;
        private final long parentOrderId;
        private final int venueId;
        private final long originalQuantity;
        private long remainingQuantity;
        private long filledQuantity;
        private OrderStatusCode status;

        private ChildState(final long childOrderId, final long parentOrderId, final int venueId,
                           final long originalQuantity, final long remainingQuantity, final long filledQuantity,
                           final OrderStatusCode status) {
            this.childOrderId = childOrderId;
            this.parentOrderId = parentOrderId;
            this.venueId = venueId;
            this.originalQuantity = originalQuantity;
            this.remainingQuantity = remainingQuantity;
            this.filledQuantity = filledQuantity;
            this.status = status;
        }

        private ChildSnapshot snapshot() {
            return new ChildSnapshot(childOrderId, parentOrderId, venueId, originalQuantity, remainingQuantity,
                    filledQuantity, status);
        }
    }
}
