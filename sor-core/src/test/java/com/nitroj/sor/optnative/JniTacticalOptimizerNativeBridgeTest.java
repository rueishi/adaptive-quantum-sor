package com.nitroj.sor.optnative;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.optimizer.IsingCudaQStrategicOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify real Java-to-native shared library integration.
 *
 * <p>Role in system: covers P2-TC-006 by loading the Gradle-built JNI library
 * and calling native echo/status functions through the JVM.</p>
 *
 * <p>Relationships: uses {@link JniTacticalOptimizerNativeBridge},
 * {@link TacticalOptimizerBufferLayout}, and native C++ implementation built by
 * CMake.</p>
 *
 * <p>Lifecycle: Gradle runs {@code nativeBuild} before this JUnit test and
 * supplies {@code sor.native.lib.dir}.</p>
 *
 * <p>Design intent: prove actual shared-library wiring without expanding into
 * production crash isolation or full native optimizer execution.</p>
 */
final class JniTacticalOptimizerNativeBridgeTest {
    @Test
    void javaLoadsGradleBuiltNativeLibraryAndCallsEcho() {
        final JniTacticalOptimizerNativeBridge bridge = bridge();

        assertEquals(123, bridge.echo(123));
    }

    @Test
    void javaReceivesNativeInvalidInputAndTimeoutStatuses() {
        final JniTacticalOptimizerNativeBridge bridge = bridge();
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult subset = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final ByteBuffer nativeInput = new TacticalOptimizerNativeInput(input, subset, 1_000L).toDirectBuffer();
        final ByteBuffer output = TacticalOptimizerBufferLayout.allocateOutput(
                input.instrumentCount * input.venueCount * input.regimeCount * input.urgencyCount);

        assertEquals(TacticalOptimizerNativeStatus.OK, bridge.optimizeStatus(nativeInput, output, 1_000L));
        assertEquals(TacticalOptimizerNativeStatus.TIMEOUT, bridge.optimizeStatus(nativeInput, output, 0L));
        assertEquals("input and output buffers must be direct", assertThrows(
                IllegalArgumentException.class,
                () -> bridge.optimizeStatus(ByteBuffer.allocate(8), output, 1_000L)
        ).getMessage());
    }

    @Test
    void missingLibraryPathFailsClearly() {
        final Path missing = Path.of("build", "native", "missing-tactical-optimizer.so");

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new JniTacticalOptimizerNativeBridge(missing));

        assertTrue(error.getMessage().contains("native library not found"));
    }

    private static JniTacticalOptimizerNativeBridge bridge() {
        final String nativeDir = System.getProperty("sor.native.lib.dir");
        assertTrue(nativeDir != null && !nativeDir.isBlank(), "sor.native.lib.dir must be configured by Gradle");
        return new JniTacticalOptimizerNativeBridge(
                JniTacticalOptimizerNativeBridge.resolveLibrary(Path.of(nativeDir))
        );
    }
}
