package com.nitroj.sor.optnative;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verify native tactical optimizer direct-buffer layout.
 *
 * <p>Role in system: covers P2-TC-002 Java writes/C++ reads, C++ writes/Java
 * reads, and schema mismatch failure behavior.</p>
 *
 * <p>Relationships: uses {@link TacticalOptimizerBufferLayout} with native
 * input/output wrappers.</p>
 *
 * <p>Lifecycle: executed as deterministic layout coverage without compiling
 * C++.</p>
 *
 * <p>Design intent: make byte order, dimensions, schema version, and index
 * formulas executable in tests instead of relying on prose only.</p>
 */
final class TacticalOptimizerBufferLayoutTest {
    @Test
    void javaWritesLayoutAndNativeProbeReadsExpectedValues() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult subset = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final ByteBuffer buffer = new TacticalOptimizerNativeInput(input, subset, 1_000L).toDirectBuffer();

        assertEquals(TacticalOptimizerBufferLayout.MAGIC, TacticalOptimizerBufferLayout.readHeaderField(buffer, 0));
        assertEquals(TacticalOptimizerBufferLayout.SCHEMA_VERSION, TacticalOptimizerBufferLayout.readHeaderField(buffer, 1));
        assertEquals(input.instrumentCount, TacticalOptimizerBufferLayout.readHeaderField(buffer, 2));
        assertEquals(input.venueCount, TacticalOptimizerBufferLayout.readHeaderField(buffer, 3));
        assertEquals(subset.subsetOffset[0], TacticalOptimizerBufferLayout.readSubsetOffset(buffer, 0));
        assertEquals(subset.selectedVenueIds[0], TacticalOptimizerBufferLayout.readSelectedVenueId(buffer, 0));
    }

    @Test
    void nativeWritesOutputAndJavaReadsExpectedValues() {
        final ByteBuffer output = TacticalOptimizerBufferLayout.allocateOutput(3);
        TacticalOptimizerBufferLayout.writeOutputWeights(output, new int[] {100, 200, 300});

        final TacticalPolicyResult result = TacticalOptimizerBufferLayout.readOutputWeights(output);

        assertEquals(3, result.venueWeightBps.length);
        assertEquals(100, result.venueWeightBps[0]);
        assertEquals(200, result.venueWeightBps[1]);
        assertEquals(300, result.venueWeightBps[2]);
    }

    @Test
    void schemaMismatchFailsClearly() {
        final ByteBuffer badInput = ByteBuffer.allocateDirect(32).order(ByteOrder.LITTLE_ENDIAN);
        badInput.putInt(TacticalOptimizerBufferLayout.MAGIC);
        badInput.putInt(TacticalOptimizerBufferLayout.SCHEMA_VERSION + 1);

        assertEquals("native input schema mismatch", assertThrows(
                IllegalArgumentException.class,
                () -> TacticalOptimizerBufferLayout.validateInputBuffer(badInput)
        ).getMessage());
        assertThrows(IllegalArgumentException.class, () -> TacticalOptimizerBufferLayout.allocateOutput(0));
        assertThrows(IllegalArgumentException.class,
                () -> TacticalOptimizerBufferLayout.writeOutputWeights(TacticalOptimizerBufferLayout.allocateOutput(2), new int[] {1}));
    }
}
