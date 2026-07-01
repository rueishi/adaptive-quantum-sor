package com.nitroj.sor.optnative;

import com.nitroj.sor.core.TestPolicyFixtures;
import com.nitroj.sor.core.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 2 Java/native tactical optimizer boundary.
 *
 * <p>Role in system: covers P2-TC-001 load, echo, invalid input, and missing
 * native library handling.</p>
 *
 * <p>Relationships: exercises {@link TacticalOptimizerNativeBridge},
 * {@link TacticalOptimizerNativeInput}, and {@link TacticalOptimizerNativeOutput}
 * with Phase 1 policy fixtures.</p>
 *
 * <p>Lifecycle: executed by Gradle as deterministic no-hardware native boundary
 * coverage.</p>
 *
 * <p>Design intent: prove the Java side of the ABI is stable before CUDA
 * hardware-specific execution is required.</p>
 */
final class TacticalOptimizerNativeBridgeTest {
    @Test
    void nativeBridgeLoadsAndEchoCallWorks() {
        final TacticalOptimizerNativeBridge bridge = new TacticalOptimizerNativeBridge();

        assertTrue(bridge.load());
        assertEquals(42, bridge.echo(42));
    }

    @Test
    void optimizeReturnsTacticalResultForValidInput() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult subset = new IsingCudaQStrategicOptimizerStub(2).optimize(input);

        final TacticalOptimizerNativeOutput output = new TacticalOptimizerNativeBridge()
                .optimize(new TacticalOptimizerNativeInput(input, subset, 1_000_000L));

        assertEquals(TacticalOptimizerNativeStatus.OK, output.statusCode());
        assertTrue(output.usable());
        assertNotNull(output.tacticalResult());
        assertEquals(input.instrumentCount * input.venueCount * input.regimeCount * input.urgencyCount,
                output.tacticalResult().venueWeightBps.length);
        assertTrue(output.nativeRuntimeNanos() > 0);
    }

    @Test
    void invalidInputReturnsErrorOrThrowsBeforeNativeCall() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult subset = new StrategicVenueSubsetResult();
        subset.subsetOffset = new int[] {0};
        subset.selectedVenueIds = new short[0];

        assertEquals("input and subset must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new TacticalOptimizerNativeInput(null, subset, 1L)
        ).getMessage());
        assertEquals("subsetOffset length must equal routeCount + 1", assertThrows(
                IllegalArgumentException.class,
                () -> new TacticalOptimizerNativeInput(input, subset, 1L)
        ).getMessage());
        assertEquals(TacticalOptimizerNativeStatus.INVALID_INPUT,
                new TacticalOptimizerNativeBridge().optimize(null).statusCode());
    }

    @Test
    void missingLibraryIsHandledWithoutNativeCrash() {
        final TacticalOptimizerNativeBridge bridge = TacticalOptimizerNativeBridge.missingLibrary();

        assertFalse(bridge.load());
        assertEquals("native library missing", assertThrows(
                IllegalStateException.class,
                () -> bridge.echo(1)
        ).getMessage());

        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult subset = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final TacticalOptimizerNativeOutput output = bridge.optimize(new TacticalOptimizerNativeInput(input, subset, 1_000L));

        assertEquals(TacticalOptimizerNativeStatus.LIBRARY_MISSING, output.statusCode());
        assertFalse(output.usable());
        assertTrue(output.diagnostics().contains("missing"));
    }
}
