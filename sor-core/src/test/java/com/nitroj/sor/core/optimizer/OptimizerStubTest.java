package com.nitroj.sor.core.optimizer;

import com.nitroj.sor.core.TestPolicyFixtures;
import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 1 strategic and tactical optimizer stubs.
 *
 * <p>Role in system: covers P1-TC-011 optimizer behavior, bounded tactical
 * output, deterministic failure simulation, and candidate updates.</p>
 *
 * <p>Relationships: exercises optimizer stubs with stats/model/risk fixtures
 * from prior task cards.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct unit coverage for optimizer stubs.</p>
 *
 * <p>Design intent: prove Java-only optimizer stubs are deterministic and safe
 * without anticipating real CUDA/CUDA-Q implementations.</p>
 */
final class OptimizerStubTest {
    @Test
    void strategicStubReturnsValidVenueSubset() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult result = new IsingCudaQStrategicOptimizerStub(2).optimize(input);

        assertEquals(input.instrumentCount * input.regimeCount * input.urgencyCount + 1, result.subsetOffset.length);
        assertTrue(result.selectedVenueIds.length > 0);
        for (short venueId : result.selectedVenueIds) {
            assertTrue(venueId >= 0 && venueId < input.venueCount);
        }
    }

    @Test
    void invalidStatsDoNotBreakOptimizer() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        input.venueStats = null;
        input.modelSignals = null;

        final StrategicVenueSubsetResult result = new IsingCudaQStrategicOptimizerStub(2).optimize(input);

        assertEquals(0, result.selectedVenueIds[0]);
    }

    @Test
    void tacticalResultValuesAreBoundedAndCanUpdateCandidate() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizerStub optimizer = new CudaTacticalOptimizerStub();
        final TacticalPolicyResult tactical = optimizer.optimize(strategic, input);
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(input.instrumentCount, input.venueCount, input.regimeCount, input.urgencyCount);

        optimizer.applyToCandidate(candidate, strategic, input, tactical);

        assertEquals(input.instrumentCount * input.venueCount * input.regimeCount * input.urgencyCount, tactical.venueWeightBps.length);
        assertTrue(tactical.fillProbabilityBps[0] >= 0 && tactical.fillProbabilityBps[0] <= 10_000);
        assertTrue(tactical.maxParticipationBps[0] >= 0 && tactical.maxParticipationBps[0] <= 10_000);
        assertTrue(candidate.venueEligible[0]);
    }

    @Test
    void failureAndTimeoutSimulationThrowsClearErrors() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);

        assertEquals("strategic optimizer failure simulated", assertThrows(
                IllegalStateException.class,
                () -> new IsingCudaQStrategicOptimizerStub(2, true).optimize(input)
        ).getMessage());
        assertEquals("tactical optimizer failure simulated", assertThrows(
                IllegalStateException.class,
                () -> new CudaTacticalOptimizerStub(true).optimize(strategic, input)
        ).getMessage());
    }
}
