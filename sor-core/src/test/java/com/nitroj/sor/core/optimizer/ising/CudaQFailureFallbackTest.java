package com.nitroj.sor.core.optimizer.ising;

import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.optnative.StrategicOptimizerNativeBridge;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.optimizer.strategic.CudaQStrategicOptimizer;
import com.nitroj.sor.core.optimizer.strategic.StrategicResultStore;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify CUDA-Q failure, timeout, and fallback behavior.
 *
 * <p>Role in system: covers P3-TC-004 by proving empty/invalid subsets are
 * rejected, backend-unavailable fallback is safe, timeout is recorded, and
 * lifecycle evidence is available.</p>
 *
 * <p>Relationships: exercises {@link CudaQFallbackPolicy},
 * {@link CudaQOptimizerHealth}, and native strategic result validation.</p>
 *
 * <p>Lifecycle: executed by Gradle with the Phase 3 unit tests.</p>
 *
 * <p>Design intent: failure paths are explicit because strategic optimization
 * must never publish unsafe route structures.</p>
 */
final class CudaQFailureFallbackTest {
    @Test
    void emptySubsetRejected() {
        final StrategicOptimizerNativeBridge bridge = bridge();

        assertEquals("strategic optimizer native result selected no venues", assertThrows(IllegalStateException.class,
                () -> bridge.validate(StrategicOptimizerNativeBridge.NativeResult.success(new short[0], 0L), 2)
        ).getMessage());
    }

    @Test
    void invalidVenueRejected() {
        final StrategicOptimizerNativeBridge bridge = bridge();

        assertEquals("strategic optimizer native result contains invalid venueId 3", assertThrows(IllegalStateException.class,
                () -> bridge.validate(StrategicOptimizerNativeBridge.NativeResult.success(new short[]{3}, 0L), 2)
        ).getMessage());
    }

    @Test
    void backendUnavailableFallbackWorksWithPreviousApprovedResult() {
        final StrategicResultStore store = new StrategicResultStore();
        final StrategicVenueSubsetResult previous = new CudaQStrategicOptimizer(bridge(), store, 1).optimize(input(1L));
        final CudaQOptimizerHealth health = new CudaQOptimizerHealth();
        health.recordBackendUnavailable();

        final StrategicVenueSubsetResult fallback = CudaQFallbackPolicy.previousThenStub(1)
                .fallback(input(2L), store.latestApproved().orElseThrow(), health);

        assertSame(previous, fallback);
        assertFalse(health.backendAvailable());
        assertEquals(1, health.failureCount());
        assertEquals(1, health.fallbackCount());
        assertEquals("CUDA-Q fallback reused previous strategic result", health.lastMessage());
    }

    @Test
    void backendUnavailableFallbackWorksWithStubWhenNoPreviousResultExists() {
        final CudaQOptimizerHealth health = new CudaQOptimizerHealth();
        health.recordBackendUnavailable();

        final StrategicVenueSubsetResult fallback = CudaQFallbackPolicy.previousThenStub(1)
                .fallback(input(2L), null, health);

        assertEquals(1, fallback.selectedVenueIds.length);
        assertEquals(1, health.fallbackCount());
        assertEquals("CUDA-Q fallback used strategic stub", health.lastMessage());
    }

    @Test
    void timeoutHandledSafelyWhenFallbackDisabled() {
        final CudaQOptimizerHealth health = new CudaQOptimizerHealth();
        health.recordTimeout();

        assertEquals("CUDA-Q fallback disabled", assertThrows(IllegalStateException.class,
                () -> CudaQFallbackPolicy.failClosed().fallback(input(3L), null, health)
        ).getMessage());
        assertTrue(health.timedOut());
        assertEquals(2, health.failureCount());
        final LifecycleEvent event = health.toLifecycleEvent(1L, 10L, 3L);
        assertEquals("CUDA-Q fallback disabled", event.message);
        assertEquals(3L, event.correlationId);
    }

    private static StrategicOptimizerNativeBridge bridge() {
        return new StrategicOptimizerNativeBridge(Path.of(System.getProperty("sor.native.lib.dir")));
    }

    private static PolicyOptimizationInput input(final long snapshotId) {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = snapshotId;
        input.createdAtNanos = snapshotId * 10L;
        input.instrumentCount = 1;
        input.venueCount = 2;
        input.regimeCount = 1;
        input.urgencyCount = 1;
        return input;
    }
}
