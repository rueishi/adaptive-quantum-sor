package com.nitroj.sor.core.optimizer.batch;

import java.util.Arrays;

/**
 * Responsibility: define one cross-parent batch venue allocation objective.
 *
 * <p>Role in system: Phase 6 turns a set of concurrent parent orders into a
 * constrained quadratic assignment problem. Linear costs score each parent /
 * venue choice, same-venue pair costs model self-impact, and correlated-venue
 * pair costs model information leakage across venues.</p>
 */
public final class BatchAllocationProblem {
    private final long batchAllocationRunId;
    private final long inputSnapshotId;
    private final long createdAtNanos;
    private final BatchParentOrderSnapshot[] parents;
    private final int venueCount;
    private final long unitQuantity;
    private final long[][] linearCost;
    private final long[] venueCapacity;
    private final long[] venueParticipationCap;
    private final long[][][] sameVenuePairCost;
    private final long[][] venueCorrelationPairCost;

    public BatchAllocationProblem(
            final long batchAllocationRunId,
            final long inputSnapshotId,
            final long createdAtNanos,
            final BatchParentOrderSnapshot[] parents,
            final int venueCount,
            final long unitQuantity,
            final long[][] linearCost,
            final long[] venueCapacity,
            final long[] venueParticipationCap,
            final long[][][] sameVenuePairCost,
            final long[][] venueCorrelationPairCost
    ) {
        if (batchAllocationRunId <= 0L) {
            throw new IllegalArgumentException("batchAllocationRunId must be positive");
        }
        if (inputSnapshotId <= 0L) {
            throw new IllegalArgumentException("inputSnapshotId must be positive");
        }
        if (parents == null || parents.length == 0) {
            throw new IllegalArgumentException("parents must not be empty");
        }
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        if (unitQuantity <= 0L) {
            throw new IllegalArgumentException("unitQuantity must be positive");
        }
        this.batchAllocationRunId = batchAllocationRunId;
        this.inputSnapshotId = inputSnapshotId;
        this.createdAtNanos = createdAtNanos;
        this.parents = Arrays.copyOf(parents, parents.length);
        this.venueCount = venueCount;
        this.unitQuantity = unitQuantity;
        this.linearCost = copyMatrix("linearCost", linearCost, parents.length, venueCount);
        this.venueCapacity = copyVenueVector("venueCapacity", venueCapacity, venueCount);
        this.venueParticipationCap = copyVenueVector("venueParticipationCap", venueParticipationCap, venueCount);
        this.sameVenuePairCost = copyPairCube(sameVenuePairCost, parents.length, venueCount);
        this.venueCorrelationPairCost = copyMatrix("venueCorrelationPairCost", venueCorrelationPairCost, venueCount, venueCount);
        validateParentQuantities();
    }

    public BatchAllocationConstraintReport evaluate(final long[][] quantities) {
        final long[][] allocation = copyMatrix("quantities", quantities, parents.length, venueCount);
        long totalResidual = 0L;
        long maxVenueExcess = 0L;
        long maxParticipationExcess = 0L;
        for (int parent = 0; parent < parents.length; parent++) {
            long allocated = 0L;
            for (int venue = 0; venue < venueCount; venue++) {
                if (allocation[parent][venue] < 0L) {
                    throw new IllegalArgumentException("quantities must be non-negative");
                }
                allocated += allocation[parent][venue];
            }
            totalResidual += Math.abs(parents[parent].quantity() - allocated);
        }
        for (int venue = 0; venue < venueCount; venue++) {
            long allocated = 0L;
            for (int parent = 0; parent < parents.length; parent++) {
                allocated += allocation[parent][venue];
            }
            maxVenueExcess = Math.max(maxVenueExcess, Math.max(0L, allocated - venueCapacity[venue]));
            maxParticipationExcess = Math.max(maxParticipationExcess, Math.max(0L, allocated - venueParticipationCap[venue]));
        }
        return new BatchAllocationConstraintReport(totalResidual, maxVenueExcess, maxParticipationExcess);
    }

    public long objectiveCost(final long[][] quantities) {
        final long[][] allocation = copyMatrix("quantities", quantities, parents.length, venueCount);
        long cost = 0L;
        for (int parent = 0; parent < parents.length; parent++) {
            for (int venue = 0; venue < venueCount; venue++) {
                cost += linearCost[parent][venue] * units(allocation[parent][venue]);
            }
        }
        for (int leftParent = 0; leftParent < parents.length; leftParent++) {
            for (int rightParent = leftParent + 1; rightParent < parents.length; rightParent++) {
                for (int leftVenue = 0; leftVenue < venueCount; leftVenue++) {
                    final long leftQty = allocation[leftParent][leftVenue];
                    if (leftQty == 0L) {
                        continue;
                    }
                    for (int rightVenue = 0; rightVenue < venueCount; rightVenue++) {
                        final long coupledQty = Math.min(leftQty, allocation[rightParent][rightVenue]);
                        if (coupledQty == 0L) {
                            continue;
                        }
                        if (leftVenue == rightVenue) {
                            cost += sameVenuePairCost[leftParent][rightParent][leftVenue] * units(coupledQty);
                        } else {
                            cost += venueCorrelationPairCost[leftVenue][rightVenue] * units(coupledQty);
                        }
                    }
                }
            }
        }
        return cost;
    }

    public long batchAllocationRunId() {
        return batchAllocationRunId;
    }

    public long inputSnapshotId() {
        return inputSnapshotId;
    }

    public long createdAtNanos() {
        return createdAtNanos;
    }

    public BatchParentOrderSnapshot[] parents() {
        return Arrays.copyOf(parents, parents.length);
    }

    public int parentCount() {
        return parents.length;
    }

    public int venueCount() {
        return venueCount;
    }

    public long unitQuantity() {
        return unitQuantity;
    }

    public long parentQuantity(final int parentIndex) {
        return parents[parentIndex].quantity();
    }

    public long venueCapacity(final int venueId) {
        return venueCapacity[venueId];
    }

    public long venueParticipationCap(final int venueId) {
        return venueParticipationCap[venueId];
    }

    public long linearCost(final int parentIndex, final int venueId) {
        return linearCost[parentIndex][venueId];
    }

    public long sameVenuePairCost(final int leftParent, final int rightParent, final int venueId) {
        return sameVenuePairCost[leftParent][rightParent][venueId];
    }

    public long venueCorrelationPairCost(final int leftVenue, final int rightVenue) {
        return venueCorrelationPairCost[leftVenue][rightVenue];
    }

    private long units(final long quantity) {
        return quantity / unitQuantity;
    }

    private void validateParentQuantities() {
        for (BatchParentOrderSnapshot parent : parents) {
            if (parent.quantity() % unitQuantity != 0L) {
                throw new IllegalArgumentException("parent quantity must be divisible by unitQuantity");
            }
        }
        for (long capacity : venueCapacity) {
            if (capacity % unitQuantity != 0L) {
                throw new IllegalArgumentException("venueCapacity values must be divisible by unitQuantity");
            }
        }
        for (long cap : venueParticipationCap) {
            if (cap % unitQuantity != 0L) {
                throw new IllegalArgumentException("venueParticipationCap values must be divisible by unitQuantity");
            }
        }
    }

    private static long[] copyVenueVector(final String name, final long[] source, final int venueCount) {
        if (source == null || source.length != venueCount) {
            throw new IllegalArgumentException(name + " length must equal venueCount");
        }
        final long[] copy = Arrays.copyOf(source, source.length);
        for (long value : copy) {
            if (value < 0L) {
                throw new IllegalArgumentException(name + " values must be non-negative");
            }
        }
        return copy;
    }

    private static long[][] copyMatrix(final String name, final long[][] source, final int rows, final int cols) {
        if (source == null || source.length != rows) {
            throw new IllegalArgumentException(name + " row count mismatch");
        }
        final long[][] copy = new long[rows][cols];
        for (int row = 0; row < rows; row++) {
            if (source[row] == null || source[row].length != cols) {
                throw new IllegalArgumentException(name + " column count mismatch");
            }
            copy[row] = Arrays.copyOf(source[row], cols);
        }
        return copy;
    }

    private static long[][][] copyPairCube(final long[][][] source, final int parentCount, final int venueCount) {
        if (source == null || source.length != parentCount) {
            throw new IllegalArgumentException("sameVenuePairCost parent count mismatch");
        }
        final long[][][] copy = new long[parentCount][parentCount][venueCount];
        for (int left = 0; left < parentCount; left++) {
            if (source[left] == null || source[left].length != parentCount) {
                throw new IllegalArgumentException("sameVenuePairCost parent count mismatch");
            }
            for (int right = 0; right < parentCount; right++) {
                if (source[left][right] == null || source[left][right].length != venueCount) {
                    throw new IllegalArgumentException("sameVenuePairCost venue count mismatch");
                }
                copy[left][right] = Arrays.copyOf(source[left][right], venueCount);
            }
        }
        return copy;
    }
}
