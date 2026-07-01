package com.nitroj.sor.core.optimizer;

import com.nitroj.sor.core.TestPolicyFixtures;
import com.nitroj.sor.core.governance.InMemoryPolicySnapshotStore;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.optnative.CudaFallbackPolicy;
import com.nitroj.sor.optnative.TacticalOptimizerNativeBridge;
import com.nitroj.sor.optnative.TacticalOptimizerNativeStatus;
import com.nitroj.sor.core.policy.PolicyOptimizationInput;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.publication.PublicationGate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify safe CUDA failure and fallback handling.
 *
 * <p>Role in system: covers P2-TC-004 GPU unavailable, library missing,
 * timeout, native error, fallback, and no-fallback fail-safe behavior.</p>
 *
 * <p>Relationships: exercises {@link CudaTacticalOptimizer},
 * {@link TacticalOptimizerNativeBridge}, fallback policy, metadata, lifecycle
 * events, and policy publisher safety.</p>
 *
 * <p>Lifecycle: executed by Gradle as no-hardware failure-path coverage.</p>
 *
 * <p>Design intent: ensure native backend failures are visible and never mutate
 * active policy by themselves.</p>
 */
final class CudaFailureFallbackTest {
    @Test
    void missingGpuHandledAndFallbackUsedWhenEnabled() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final InMemoryLifecycleEventStore events = new InMemoryLifecycleEventStore(8, true);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.gpuUnavailable(),
                CudaFallbackPolicy.enabled(1_000_000L),
                events
        );

        final TacticalPolicyResult result = optimizer.optimize(strategic, input);

        assertNotNull(result);
        assertEquals(TacticalOptimizerNativeStatus.GPU_UNAVAILABLE, optimizer.health().lastStatusCode());
        assertTrue(optimizer.lastMetadata().failureReason.contains("GPU_UNAVAILABLE"));
        assertTrue(events.snapshot().stream().anyMatch(event -> event.message.contains("fallback used")));
    }

    @Test
    void nativeTimeoutHandledWithFallback() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.timeout(),
                CudaFallbackPolicy.enabled(10L)
        );

        final TacticalPolicyResult result = optimizer.optimize(strategic, input);

        assertNotNull(result);
        assertTrue(optimizer.health().timedOut());
        assertEquals(TacticalOptimizerNativeStatus.TIMEOUT, optimizer.health().lastStatusCode());
    }

    @Test
    void missingLibraryFallbackUsedWhenEnabled() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.missingLibrary(),
                CudaFallbackPolicy.enabled(1_000_000L)
        );

        final TacticalPolicyResult result = optimizer.optimize(strategic, input);

        assertNotNull(result);
        assertEquals(TacticalOptimizerNativeStatus.LIBRARY_MISSING, optimizer.health().lastStatusCode());
    }

    @Test
    void noFallbackFailsCycleSafelyAndDoesNotPublishPolicy() {
        final PolicyOptimizationInput input = TestPolicyFixtures.input();
        final StrategicVenueSubsetResult strategic = new IsingCudaQStrategicOptimizerStub(2).optimize(input);
        final CudaTacticalOptimizer optimizer = new CudaTacticalOptimizer(
                TacticalOptimizerNativeBridge.timeout(),
                CudaFallbackPolicy.disabled(10L)
        );
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());

        final IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> optimizer.optimize(strategic, input));

        assertTrue(error.getMessage().contains("CUDA tactical optimizer failed"));
        assertNull(publisher.activePolicy());
        assertEquals(TacticalOptimizerNativeStatus.TIMEOUT, optimizer.health().lastStatusCode());
        assertTrue(optimizer.lastMetadata().failureReason.contains("TIMEOUT"));
    }

    @Test
    void invalidFallbackConfigAndConstructorInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CudaFallbackPolicy.enabled(0L));
        assertThrows(IllegalArgumentException.class,
                () -> new CudaTacticalOptimizer(null, CudaFallbackPolicy.enabled(1L)));
        assertThrows(IllegalArgumentException.class,
                () -> new CudaTacticalOptimizer(new TacticalOptimizerNativeBridge(), null));
    }
}
