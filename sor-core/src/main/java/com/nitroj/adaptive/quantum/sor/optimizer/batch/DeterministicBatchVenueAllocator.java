package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/**
 * Responsibility: deterministic small-problem reference solver for Phase 6.
 *
 * <p>Design intent: this solver enumerates unit-sized venue assignments in
 * venue-ID order. It is intentionally simple and exact for small replay cases,
 * giving native/cuOpt/QUBO backends a stable equivalence oracle.</p>
 */
public final class DeterministicBatchVenueAllocator implements BatchVenueAllocator {
    private long bestCost;
    private long[][] bestQuantities;

    @Override
    public BatchVenueAllocationPlan allocate(final BatchAllocationProblem problem) {
        if (problem == null) {
            throw new IllegalArgumentException("problem must not be null");
        }
        bestCost = Long.MAX_VALUE;
        bestQuantities = null;
        final int[] parentUnitCount = new int[problem.parentCount()];
        int totalUnits = 0;
        for (int parent = 0; parent < problem.parentCount(); parent++) {
            parentUnitCount[parent] = Math.toIntExact(problem.parentQuantity(parent) / problem.unitQuantity());
            totalUnits += parentUnitCount[parent];
        }
        search(problem, parentUnitCount, totalUnits, 0, new long[problem.parentCount()][problem.venueCount()]);
        if (bestQuantities == null) {
            throw new IllegalStateException("no feasible batch allocation");
        }
        return new BatchVenueAllocationPlan(
                problem.batchAllocationRunId(),
                problem.inputSnapshotId(),
                problem.createdAtNanos(),
                bestCost,
                problem.evaluate(bestQuantities),
                bestQuantities
        );
    }

    private void search(
            final BatchAllocationProblem problem,
            final int[] parentUnitCount,
            final int totalUnits,
            final int unitIndex,
            final long[][] quantities
    ) {
        if (unitIndex == totalUnits) {
            final BatchAllocationConstraintReport report = problem.evaluate(quantities);
            if (!report.feasible()) {
                return;
            }
            final long cost = problem.objectiveCost(quantities);
            if (cost < bestCost) {
                bestCost = cost;
                bestQuantities = copy(quantities);
            }
            return;
        }
        final int parent = parentForUnit(parentUnitCount, unitIndex);
        for (int venue = 0; venue < problem.venueCount(); venue++) {
            quantities[parent][venue] += problem.unitQuantity();
            if (withinVenueCaps(problem, quantities)) {
                search(problem, parentUnitCount, totalUnits, unitIndex + 1, quantities);
            }
            quantities[parent][venue] -= problem.unitQuantity();
        }
    }

    private static boolean withinVenueCaps(final BatchAllocationProblem problem, final long[][] quantities) {
        for (int venue = 0; venue < problem.venueCount(); venue++) {
            long allocated = 0L;
            for (int parent = 0; parent < problem.parentCount(); parent++) {
                allocated += quantities[parent][venue];
            }
            if (allocated > problem.venueCapacity(venue) || allocated > problem.venueParticipationCap(venue)) {
                return false;
            }
        }
        return true;
    }

    private static int parentForUnit(final int[] parentUnitCount, final int unitIndex) {
        int offset = 0;
        for (int parent = 0; parent < parentUnitCount.length; parent++) {
            offset += parentUnitCount[parent];
            if (unitIndex < offset) {
                return parent;
            }
        }
        throw new IllegalArgumentException("unitIndex out of range");
    }

    private static long[][] copy(final long[][] source) {
        final long[][] copy = new long[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, source[row].length);
        }
        return copy;
    }
}
