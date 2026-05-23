package com.nitroj.sor.optnative;

import com.nitroj.adaptive.quantum.sor.optimizer.ising.QuboObjectiveConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the Phase 3 strategic native backend bridge.
 *
 * <p>Role in system: covers P3-TC-002 by proving the Gradle-built backend is
 * discoverable, small QUBO inputs return valid subsets, and invalid native
 * results are rejected.</p>
 *
 * <p>Relationships: consumes {@link QuboObjectiveConfig} and
 * {@link StrategicOptimizerNativeBridge.NativeResult}.</p>
 *
 * <p>Lifecycle: executed by Gradle after nativeBuild has produced the shared
 * strategic optimizer library.</p>
 *
 * <p>Design intent: a tiny deterministic case keeps Java/native integration
 * evidence stable without claiming production CUDA-Q behavior.</p>
 */
final class StrategicOptimizerNativeBridgeTest {
    @Test
    void backendLoadsFromGradleNativeDirectory() {
        final StrategicOptimizerNativeBridge bridge = bridge();

        assertTrue(bridge.backendAvailable());
        assertTrue(bridge.libraryPath().getFileName().toString().contains("cudaq_strategic_optimizer"));
    }

    @Test
    void simpleSmallQuboReturnsValidSubset() {
        final QuboObjectiveConfig objective = new QuboObjectiveConfig(
                0, 0, 0, 3, 1, 2, 100,
                new int[]{-10, -50, -20},
                new int[9]
        );

        final StrategicOptimizerNativeBridge.NativeResult result = bridge().optimize(objective);

        assertEquals(StrategicOptimizerNativeBridge.StrategicOptimizerNativeStatus.OK, result.status());
        assertArrayEquals(new short[]{1, 2}, result.selectedVenueIds());
        assertEquals(-70L, result.objectiveEnergy());
        bridge().validate(result, 3);
    }

    @Test
    void pairPenaltyCanOverrideIndependentVenueRanking() {
        final int[] pair = new int[9];
        pair[1] = 1_000;
        pair[3] = 1_000;
        final QuboObjectiveConfig objective = new QuboObjectiveConfig(
                0, 0, 0, 3, 1, 2, 100,
                new int[]{-100, -90, -80},
                pair
        );

        final StrategicOptimizerNativeBridge.NativeResult result = bridge().optimize(objective);

        assertEquals(StrategicOptimizerNativeBridge.StrategicOptimizerNativeStatus.OK, result.status());
        assertArrayEquals(new short[]{0, 2}, result.selectedVenueIds());
        assertEquals(-180L, result.objectiveEnergy());
    }

    @Test
    void invalidNativeResultRejected() {
        final StrategicOptimizerNativeBridge bridge = bridge();

        assertEquals("strategic optimizer native result selected no venues", assertThrows(IllegalStateException.class,
                () -> bridge.validate(StrategicOptimizerNativeBridge.NativeResult.success(new short[0], 0L), 3)
        ).getMessage());
        assertEquals("strategic optimizer native result contains invalid venueId 9", assertThrows(IllegalStateException.class,
                () -> bridge.validate(StrategicOptimizerNativeBridge.NativeResult.success(new short[]{9}, 0L), 3)
        ).getMessage());
        assertEquals("strategic optimizer native result contains duplicate venueId 1", assertThrows(IllegalStateException.class,
                () -> bridge.validate(StrategicOptimizerNativeBridge.NativeResult.success(new short[]{1, 1}, 0L), 3)
        ).getMessage());
    }

    @Test
    void missingBackendReportsClearLoadFailure() {
        assertTrue(assertThrows(IllegalStateException.class,
                () -> new StrategicOptimizerNativeBridge(Path.of("missing-native-dir"))
        ).getMessage().contains("strategic optimizer native library missing"));
    }

    private static StrategicOptimizerNativeBridge bridge() {
        return new StrategicOptimizerNativeBridge(Path.of(System.getProperty("sor.native.lib.dir")));
    }
}
