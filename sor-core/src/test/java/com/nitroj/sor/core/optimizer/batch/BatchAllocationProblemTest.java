package com.nitroj.sor.core.optimizer.batch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 6 batch allocation model contracts.
 */
final class BatchAllocationProblemTest {
    @Test
    void modelCapturesLineageConstraintsAndQuadraticCosts() {
        final BatchAllocationProblem problem = problem();

        final long[][] independent = {
                {100, 0},
                {100, 0}
        };
        final long[][] diversified = {
                {100, 0},
                {0, 100}
        };

        assertEquals(11L, problem.batchAllocationRunId());
        assertEquals(7L, problem.inputSnapshotId());
        assertEquals(2, problem.parentCount());
        assertEquals(2, problem.venueCount());
        assertEquals(100L, problem.unitQuantity());
        assertEquals(100L, problem.parentQuantity(0));
        assertEquals(200L, problem.venueCapacity(0));
        assertEquals(200L, problem.venueParticipationCap(0));
        assertEquals(1L, problem.linearCost(0, 0));
        assertEquals(30L, problem.sameVenuePairCost(0, 1, 0));
        assertEquals(5L, problem.venueCorrelationPairCost(0, 1));
        assertTrue(problem.evaluate(independent).feasible());
        assertEquals(32L, problem.objectiveCost(independent));
        assertEquals(9L, problem.objectiveCost(diversified));
    }

    @Test
    void constraintReportFlagsResidualAndCapacityExcess() {
        final BatchAllocationProblem problem = problem();

        final BatchAllocationConstraintReport residual = problem.evaluate(new long[][]{
                {100, 0},
                {0, 0}
        });
        final BatchAllocationProblem tightCapacity = new BatchAllocationProblem(
                12L,
                8L,
                99L,
                parents(),
                2,
                100,
                new long[][]{{1, 3}, {1, 3}},
                new long[]{100, 100},
                new long[]{100, 100},
                sameVenueCost(2, 2, 30),
                new long[][]{{0, 5}, {5, 0}}
        );
        final BatchAllocationConstraintReport capacity = tightCapacity.evaluate(new long[][]{
                {100, 100},
                {100, 100}
        });

        assertFalse(residual.feasible());
        assertEquals(100L, residual.parentResidualQty());
        assertFalse(capacity.feasible());
        assertEquals(200L, capacity.parentResidualQty());
        assertEquals(100L, capacity.maxVenueCapacityExcess());
    }

    @Test
    void invalidModelInputsFailEarly() {
        assertEquals("parentOrderId must be positive", assertThrows(IllegalArgumentException.class,
                () -> new BatchParentOrderSnapshot(0L, 0, 1, 100)
        ).getMessage());
        assertEquals("side must be BUY(1) or SELL(2)", assertThrows(IllegalArgumentException.class,
                () -> new BatchParentOrderSnapshot(1L, 0, 3, 100)
        ).getMessage());
        assertEquals("parent quantity must be divisible by unitQuantity", assertThrows(IllegalArgumentException.class,
                () -> new BatchAllocationProblem(
                        1L,
                        1L,
                        1L,
                        new BatchParentOrderSnapshot[]{
                                new BatchParentOrderSnapshot(101L, 0, 1, 150),
                                new BatchParentOrderSnapshot(102L, 0, 1, 100)
                        },
                        2,
                        100,
                        new long[][]{{1, 2}, {1, 2}},
                        new long[]{200, 200},
                        new long[]{200, 200},
                        sameVenueCost(2, 2, 0),
                        new long[][]{{0, 0}, {0, 0}}
                )
        ).getMessage());
    }

    @Test
    void planDefensivelyCopiesQuantities() {
        final long[][] quantities = {
                {100, 0},
                {0, 100}
        };
        final BatchVenueAllocationPlan plan = new BatchVenueAllocationPlan(
                11L,
                7L,
                99L,
                7L,
                new BatchAllocationConstraintReport(0, 0, 0),
                quantities
        );

        quantities[0][0] = 0L;
        final long[][] copy = plan.parentVenueQuantities();
        copy[1][1] = 0L;

        assertArrayEquals(new long[]{100L, 0L}, plan.parentVenueQuantities()[0]);
        assertArrayEquals(new long[]{0L, 100L}, plan.parentVenueQuantities()[1]);
        assertTrue(plan.constraintReport().feasible());
    }

    static BatchAllocationProblem problem() {
        return new BatchAllocationProblem(
                11L,
                7L,
                99L,
                parents(),
                2,
                100,
                new long[][]{
                        {1, 3},
                        {1, 3}
                },
                new long[]{200, 200},
                new long[]{200, 200},
                sameVenueCost(2, 2, 30),
                new long[][]{
                        {0, 5},
                        {5, 0}
                }
        );
    }

    private static BatchParentOrderSnapshot[] parents() {
        return new BatchParentOrderSnapshot[]{
                new BatchParentOrderSnapshot(101L, 0, 1, 100),
                new BatchParentOrderSnapshot(102L, 0, 1, 100)
        };
    }

    static long[][][] sameVenueCost(final int parentCount, final int venueCount, final long cost) {
        final long[][][] values = new long[parentCount][parentCount][venueCount];
        for (int left = 0; left < parentCount; left++) {
            for (int right = 0; right < parentCount; right++) {
                for (int venue = 0; venue < venueCount; venue++) {
                    values[left][right][venue] = cost;
                }
            }
        }
        return values;
    }
}
