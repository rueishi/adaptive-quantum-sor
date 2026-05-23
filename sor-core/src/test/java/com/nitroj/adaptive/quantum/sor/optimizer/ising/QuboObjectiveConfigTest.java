package com.nitroj.adaptive.quantum.sor.optimizer.ising;

import com.nitroj.adaptive.quantum.sor.model.ModelSignalState;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import com.nitroj.adaptive.quantum.sor.stats.VenueStatsState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 3 QUBO objective construction and penalties.
 *
 * <p>Role in system: covers P3-TC-001 by proving simple route objectives have
 * deterministic coefficients and invalid strategic subsets are penalized.</p>
 *
 * <p>Relationships: builds {@link QuboObjectiveConfig} from
 * {@link PolicyOptimizationInput} snapshots.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: small hand-computed cases keep the QUBO scoring contract
 * readable as later native backends consume it.</p>
 */
final class QuboObjectiveConfigTest {
    @Test
    void objectiveBuilderCreatesExpectedCoefficientsForSimpleCase() {
        final PolicyOptimizationInput input = input();
        input.modelSignals.setVenueScoreBps(0, 0, 0, 6_000);
        input.modelSignals.setVenueScoreBps(0, 1, 0, 5_000);
        input.venueStats.update(0, 0, 0, 20_000, 8_000, 100, 200, 2, 300);
        input.venueStats.update(0, 1, 0, 10_000, 6_000, 1_000, 500, 5, 700);

        final QuboObjectiveConfig objective = QuboObjectiveConfig.fromInput(input, 0, 0, 0, 1);

        assertEquals(2, objective.venueCount());
        assertEquals(1, objective.maxSubsetSize());
        assertEquals(-6_736, objective.linearCoefficient(0));
        assertEquals(-5_374, objective.linearCoefficient(1));
        assertEquals(370, objective.pairCoefficient(0, 1));
        assertEquals(370, objective.pairCoefficient(1, 0));
        assertTrue(objective.energy(new boolean[]{true, false}) < objective.energy(new boolean[]{false, true}));
    }

    @Test
    void builderPopulatesPairPenaltiesThatCanChangeBestSubset() {
        final PolicyOptimizationInput input = input(3);
        input.modelSignals.setVenueScoreBps(0, 0, 0, 10_000);
        input.modelSignals.setVenueScoreBps(0, 1, 0, 9_900);
        input.modelSignals.setVenueScoreBps(0, 2, 0, 9_100);
        input.venueStats.update(0, 0, 0, 10_000, 5_000, 3_000, 2_000, 0, 0);
        input.venueStats.update(0, 1, 0, 10_000, 5_000, 3_000, 2_000, 0, 0);
        input.venueStats.update(0, 2, 0, 10_000, 5_000, 0, 0, 0, 0);

        final QuboObjectiveConfig objective = QuboObjectiveConfig.fromInput(input, 0, 0, 0, 2);

        assertEquals(1_500, objective.pairCoefficient(0, 1));
        assertTrue(objective.energy(new boolean[]{true, false, true})
                < objective.energy(new boolean[]{true, true, false}));
    }

    @Test
    void constraintsPenalizeInvalidSubsets() {
        final QuboObjectiveConfig objective = new QuboObjectiveConfig(
                0, 0, 0, 3, 1, 2, 100,
                new int[]{-10, -20, -30},
                new int[9]
        );

        assertFalse(objective.validSubset(new boolean[]{false, false, false}));
        assertFalse(objective.validSubset(new boolean[]{true, true, true}));
        assertTrue(objective.validSubset(new boolean[]{false, true, true}));
        assertEquals(100L, objective.energy(new boolean[]{false, false, false}));
        assertEquals(40L, objective.energy(new boolean[]{true, true, true}));
        assertEquals(-50L, objective.energy(new boolean[]{false, true, true}));
    }

    @Test
    void constructorRejectsInvalidBoundaryValues() {
        assertEquals("venueCount must be positive", assertThrows(IllegalArgumentException.class,
                () -> new QuboObjectiveConfig(0, 0, 0, 0, 0, 0, 1, new int[0], new int[0])
        ).getMessage());
        assertEquals("linearCoefficients length must equal venueCount", assertThrows(IllegalArgumentException.class,
                () -> new QuboObjectiveConfig(0, 0, 0, 2, 1, 1, 1, new int[1], new int[4])
        ).getMessage());
        assertEquals("selected length must equal venueCount", assertThrows(IllegalArgumentException.class,
                () -> new QuboObjectiveConfig(0, 0, 0, 2, 1, 1, 1, new int[2], new int[4])
                        .energy(new boolean[]{true})
        ).getMessage());
    }

    private static PolicyOptimizationInput input() {
        return input(2);
    }

    private static PolicyOptimizationInput input(final int venueCount) {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.instrumentCount = 1;
        input.venueCount = venueCount;
        input.regimeCount = 1;
        input.urgencyCount = 1;
        input.modelSignals = new ModelSignalState(1, venueCount, 1);
        input.venueStats = new VenueStatsState(1, venueCount, 1);
        return input;
    }
}
