package com.nitroj.adaptive.quantum.sor.optimizer.strategic;

import com.nitroj.adaptive.quantum.sor.model.ModelSignalState;
import com.nitroj.sor.optnative.StrategicOptimizerNativeBridge;
import com.nitroj.adaptive.quantum.sor.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify CUDA-Q strategic result integration.
 *
 * <p>Role in system: covers P3-TC-003 by proving approved strategic results are
 * versioned and consumed by tactical optimization while failed attempts do not
 * replace the approved store state.</p>
 *
 * <p>Relationships: exercises {@link CudaQStrategicOptimizer},
 * {@link StrategicResultStore}, and the existing tactical optimizer contract.</p>
 *
 * <p>Lifecycle: executed by Gradle after the native strategic backend is built.</p>
 *
 * <p>Design intent: the tests make the approved-result handoff explicit because
 * tactical optimization must not consume failed strategic attempts.</p>
 */
final class CudaQStrategicOptimizerTest {
    @Test
    void newApprovedResultConsumedByTacticalOptimizer() {
        final PolicyOptimizationInput input = input(1L);
        input.modelSignals.setVenueScoreBps(0, 2, 0, 9_000);
        input.modelSignals.setVenueScoreBps(0, 1, 0, 8_000);
        final StrategicResultStore store = new StrategicResultStore();
        final CudaQStrategicOptimizer optimizer = optimizer(store);

        final StrategicVenueSubsetResult strategic = optimizer.optimize(input);
        final TacticalPolicyResult tactical = new CudaTacticalOptimizerStub().optimize(store.latestApproved().orElseThrow(), input);

        assertEquals(1L, strategic.version);
        assertEquals(2, strategic.selectedVenueIds.length);
        assertEquals(1, strategic.selectedVenueIds[0]);
        assertEquals(2, strategic.selectedVenueIds[1]);
        assertEquals(strategic.version, tactical.strategicSubsetVersion);
        assertTrue(store.latestAttemptMetadata().orElseThrow().success);
    }

    @Test
    void failedResultNotConsumed() {
        final StrategicResultStore store = new StrategicResultStore();
        final StrategicVenueSubsetResult approved = optimizer(store).optimize(input(1L));
        final CudaQStrategicOptimizer failing = new CudaQStrategicOptimizer(new StrategicOptimizerNativeBridge(), store, 2);

        assertThrows(IllegalStateException.class, () -> failing.optimize(input(2L)));

        assertSame(approved, store.latestApproved().orElseThrow());
        assertFalse(store.latestAttemptMetadata().orElseThrow().success);
        assertEquals("strategic optimizer native status BACKEND_UNAVAILABLE: backend unavailable",
                store.latestAttemptMetadata().orElseThrow().failureReason);
    }

    @Test
    void unchangedSubsetPreservesCurrentStructuralPolicy() {
        final StrategicResultStore store = new StrategicResultStore();
        final CudaQStrategicOptimizer optimizer = optimizer(store);
        final StrategicVenueSubsetResult first = optimizer.optimize(input(1L));
        final StrategicVenueSubsetResult second = optimizer.optimize(input(2L));

        assertEquals(1L, first.version);
        assertEquals(2L, second.version);
        assertEquals(first.selectedVenueIds[0], second.selectedVenueIds[0]);
        assertEquals(first.selectedVenueIds[1], second.selectedVenueIds[1]);
    }

    @Test
    void storeRejectsInvalidApproval() {
        final StrategicResultStore store = new StrategicResultStore();
        final StrategicVenueSubsetResult result = new StrategicVenueSubsetResult();
        result.selectedVenueIds = new short[0];

        assertEquals("approved strategic result must select at least one venue", assertThrows(IllegalArgumentException.class,
                () -> store.approve(result, metadata())
        ).getMessage());
    }

    private static CudaQStrategicOptimizer optimizer(final StrategicResultStore store) {
        return new CudaQStrategicOptimizer(
                new StrategicOptimizerNativeBridge(Path.of(System.getProperty("sor.native.lib.dir"))),
                store,
                2
        );
    }

    private static PolicyOptimizationInput input(final long snapshotId) {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.inputSnapshotId = snapshotId;
        input.createdAtNanos = snapshotId * 10L;
        input.instrumentCount = 1;
        input.venueCount = 3;
        input.regimeCount = 1;
        input.urgencyCount = 1;
        input.modelSignals = new ModelSignalState(1, 3, 1);
        return input;
    }

    private static com.nitroj.adaptive.quantum.sor.optimizer.OptimizerRunMetadata metadata() {
        final com.nitroj.adaptive.quantum.sor.optimizer.OptimizerRunMetadata metadata =
                new com.nitroj.adaptive.quantum.sor.optimizer.OptimizerRunMetadata();
        metadata.optimizerRunId = 1L;
        metadata.success = true;
        return metadata;
    }
}
