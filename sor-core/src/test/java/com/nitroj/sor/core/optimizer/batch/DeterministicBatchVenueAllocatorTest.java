package com.nitroj.sor.core.optimizer.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the exact deterministic Phase 6 reference solver.
 */
final class DeterministicBatchVenueAllocatorTest {
    private final DeterministicBatchVenueAllocator allocator = new DeterministicBatchVenueAllocator();

    @Test
    void referenceSolverFindsLowestCostFeasibleAllocation() {
        final BatchVenueAllocationPlan plan = allocator.allocate(BatchAllocationProblemTest.problem());

        assertEquals(9L, plan.objectiveCost());
        assertTrue(plan.constraintReport().feasible());
        assertArrayEquals(new long[]{100L, 0L}, plan.parentVenueQuantities()[0]);
        assertArrayEquals(new long[]{0L, 100L}, plan.parentVenueQuantities()[1]);
    }

    @Test
    void singleParentBatchChoosesSameVenueAsIndependentRouteRanking() {
        final BatchAllocationProblem problem = new BatchAllocationProblem(
                21L,
                31L,
                41L,
                new BatchParentOrderSnapshot[]{
                        new BatchParentOrderSnapshot(201L, 0, 1, 100)
                },
                3,
                100,
                new long[][]{{20, 5, 9}},
                new long[]{100, 100, 100},
                new long[]{100, 100, 100},
                new long[1][1][3],
                new long[3][3]
        );

        final BatchVenueAllocationPlan plan = allocator.allocate(problem);

        assertEquals(5L, plan.objectiveCost());
        assertArrayEquals(new long[]{0L, 100L, 0L}, plan.parentVenueQuantities()[0]);
    }

    @Test
    void sharedVenueCapacityForcesDiversification() {
        final BatchAllocationProblem problem = new BatchAllocationProblem(
                22L,
                32L,
                42L,
                parents(),
                2,
                100,
                new long[][]{{1, 9}, {1, 9}},
                new long[]{100, 200},
                new long[]{100, 200},
                new long[2][2][2],
                new long[2][2]
        );

        final BatchVenueAllocationPlan plan = allocator.allocate(problem);

        assertEquals(10L, plan.objectiveCost());
        assertArrayEquals(new long[]{100L, 0L}, plan.parentVenueQuantities()[0]);
        assertArrayEquals(new long[]{0L, 100L}, plan.parentVenueQuantities()[1]);
    }

    @Test
    void infeasibleBatchFailsClosed() {
        final BatchAllocationProblem problem = new BatchAllocationProblem(
                23L,
                33L,
                43L,
                parents(),
                2,
                100,
                new long[][]{{1, 9}, {1, 9}},
                new long[]{0, 100},
                new long[]{0, 100},
                new long[2][2][2],
                new long[2][2]
        );

        assertEquals("no feasible batch allocation", assertThrows(IllegalStateException.class,
                () -> allocator.allocate(problem)
        ).getMessage());
    }

    @Test
    void tieBreakPrefersLowerVenueIds() {
        final BatchAllocationProblem problem = new BatchAllocationProblem(
                24L,
                34L,
                44L,
                new BatchParentOrderSnapshot[]{
                        new BatchParentOrderSnapshot(203L, 0, 1, 100)
                },
                2,
                100,
                new long[][]{{5, 5}},
                new long[]{100, 100},
                new long[]{100, 100},
                new long[1][1][2],
                new long[2][2]
        );

        final BatchVenueAllocationPlan plan = allocator.allocate(problem);

        assertArrayEquals(new long[]{100L, 0L}, plan.parentVenueQuantities()[0]);
    }

    private static BatchParentOrderSnapshot[] parents() {
        return new BatchParentOrderSnapshot[]{
                new BatchParentOrderSnapshot(201L, 0, 1, 100),
                new BatchParentOrderSnapshot(202L, 0, 1, 100)
        };
    }
}
