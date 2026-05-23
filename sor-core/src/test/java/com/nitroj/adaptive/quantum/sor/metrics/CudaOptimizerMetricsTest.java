package com.nitroj.adaptive.quantum.sor.metrics;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.sor.optnative.TacticalOptimizerNativeBridge;
import com.nitroj.sor.optnative.TacticalOptimizerNativeInput;
import com.nitroj.sor.optnative.TacticalOptimizerNativeOutput;
import com.nitroj.sor.optnative.TacticalOptimizerNativeStatus;
import com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify CUDA tactical optimizer profiling metrics.
 *
 * <p>Role in system: covers P2-TC-005 runtime measurement and Java-stub
 * comparison evidence for the Phase 2 report.</p>
 *
 * <p>Relationships: records {@link TacticalOptimizerNativeOutput} and compares
 * deterministic bridge output against the Phase 1 tactical stub.</p>
 *
 * <p>Lifecycle: executed by Gradle as metrics/reporting coverage.</p>
 *
 * <p>Design intent: provide performance evidence without requiring Nsight or
 * CUDA hardware in the Adaptive Quantum SOR build.</p>
 */
final class CudaOptimizerMetricsTest {
    @Test
    void optimizerRuntimeMeasured() {
        final CudaOptimizerMetrics metrics = new CudaOptimizerMetrics();
        metrics.record(new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.OK, null, "ok", 100L));
        metrics.record(new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.OK, null, "ok", 300L));

        assertEquals(2L, metrics.runCount());
        assertEquals(200L, metrics.averageRuntimeNanos());
        assertEquals(300L, metrics.maxRuntimeNanos());
        assertEquals(TacticalOptimizerNativeStatus.OK, metrics.lastStatusCode());
        assertTrue(metrics.renderReport().contains("averageRuntimeNanos=200"));
        assertThrows(IllegalArgumentException.class, () -> metrics.record(null));
    }

    @Test
    void policyOutputComparedAgainstJavaStub() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);

        final TacticalPolicyResult nativeResult = new TacticalOptimizerNativeBridge()
                .optimize(new TacticalOptimizerNativeInput(input, strategic, 1_000_000L))
                .tacticalResult();
        final TacticalPolicyResult javaStubResult = new CudaTacticalOptimizerStub().optimize(strategic, input);

        assertArrayEquals(javaStubResult.venueWeightBps, nativeResult.venueWeightBps);
        assertArrayEquals(javaStubResult.fillProbabilityBps, nativeResult.fillProbabilityBps);
        assertArrayEquals(javaStubResult.maxParticipationBps, nativeResult.maxParticipationBps);
    }
}
