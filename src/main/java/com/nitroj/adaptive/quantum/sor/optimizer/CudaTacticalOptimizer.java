package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;
import com.nitroj.adaptive.quantum.sor.nativebridge.CudaFallbackPolicy;
import com.nitroj.adaptive.quantum.sor.nativebridge.CudaOptimizerHealth;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeBridge;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeInput;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeOutput;
import com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeStatus;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

/**
 * Responsibility: run the Phase 2 CUDA tactical optimizer with safe fallback.
 *
 * <p>Role in system: this is the Java tactical optimizer implementation that
 * calls the native bridge and returns {@link TacticalPolicyResult} to the
 * existing policy pipeline.</p>
 *
 * <p>Relationships: consumes {@link TacticalOptimizerNativeBridge},
 * {@link CudaFallbackPolicy}, and optionally writes lifecycle events for CUDA
 * failures/fallbacks.</p>
 *
 * <p>Lifecycle: created during optimizer wiring and invoked by the warm-path
 * coordinator, never by hot route execution.</p>
 *
 * <p>Design intent: native failures must not corrupt active policy; callers get
 * either a valid tactical result from CUDA/fallback or a clear failed cycle.</p>
 */
public final class CudaTacticalOptimizer implements TacticalPolicyOptimizer {
    public static final int OPTIMIZER_TYPE = 20;

    private final TacticalOptimizerNativeBridge bridge;
    private final CudaFallbackPolicy fallbackPolicy;
    private final CudaTacticalOptimizerStub fallbackOptimizer;
    private final InMemoryLifecycleEventStore lifecycleEvents;
    private OptimizerRunMetadata lastMetadata;
    private CudaOptimizerHealth health = new CudaOptimizerHealth(false, false, false,
            TacticalOptimizerNativeStatus.LIBRARY_MISSING, "not started");
    private long nextLifecycleEventId = 1L;

    public CudaTacticalOptimizer(final TacticalOptimizerNativeBridge bridge, final CudaFallbackPolicy fallbackPolicy) {
        this(bridge, fallbackPolicy, null);
    }

    public CudaTacticalOptimizer(
            final TacticalOptimizerNativeBridge bridge,
            final CudaFallbackPolicy fallbackPolicy,
            final InMemoryLifecycleEventStore lifecycleEvents
    ) {
        if (bridge == null || fallbackPolicy == null) {
            throw new IllegalArgumentException("bridge and fallbackPolicy must not be null");
        }
        this.bridge = bridge;
        this.fallbackPolicy = fallbackPolicy;
        this.lifecycleEvents = lifecycleEvents;
        this.fallbackOptimizer = new CudaTacticalOptimizerStub();
    }

    /**
     * Runs the native backend and applies configured safe fallback behavior.
     *
     * @param subset strategic venue subset
     * @param input optimizer input snapshot
     * @return CUDA or fallback tactical result
     */
    @Override
    public TacticalPolicyResult optimize(final StrategicVenueSubsetResult subset, final PolicyOptimizationInput input) {
        if (subset == null || input == null) {
            throw new IllegalArgumentException("subset and input must not be null");
        }
        final long started = Math.max(1L, System.nanoTime());
        final OptimizerRunMetadata metadata = metadata(input, started);
        final TacticalOptimizerNativeInput nativeInput =
                new TacticalOptimizerNativeInput(input, subset, fallbackPolicy.timeoutNanos());
        final TacticalOptimizerNativeOutput output = bridge.optimize(nativeInput);
        final boolean timedOut = output.statusCode() == TacticalOptimizerNativeStatus.TIMEOUT
                || output.nativeRuntimeNanos() > fallbackPolicy.timeoutNanos();
        health = new CudaOptimizerHealth(
                output.statusCode() != TacticalOptimizerNativeStatus.LIBRARY_MISSING,
                output.statusCode() != TacticalOptimizerNativeStatus.GPU_UNAVAILABLE,
                timedOut,
                timedOut ? TacticalOptimizerNativeStatus.TIMEOUT : output.statusCode(),
                output.diagnostics()
        );
        if (output.usable() && !timedOut) {
            metadata.success = true;
            metadata.completedAtNanos = started + output.nativeRuntimeNanos();
            lastMetadata = metadata;
            return output.tacticalResult();
        }
        metadata.success = false;
        metadata.failureReason = TacticalOptimizerNativeStatus.label(health.lastStatusCode()) + ": " + output.diagnostics();
        metadata.completedAtNanos = Math.max(started, System.nanoTime());
        lastMetadata = metadata;
        lifecycle(LifecycleEventType.POLICY_REJECTED, metadata.failureReason);
        if (fallbackPolicy.fallbackEnabled()) {
            lifecycle(LifecycleEventType.POLICY_PUBLISHED, "CUDA fallback used: " + metadata.failureReason);
            return fallbackOptimizer.optimize(subset, input);
        }
        throw new IllegalStateException("CUDA tactical optimizer failed: " + metadata.failureReason);
    }

    public OptimizerRunMetadata lastMetadata() {
        return lastMetadata;
    }

    public CudaOptimizerHealth health() {
        return health;
    }

    private static OptimizerRunMetadata metadata(final PolicyOptimizationInput input, final long started) {
        final OptimizerRunMetadata metadata = new OptimizerRunMetadata();
        metadata.optimizerRunId = Math.max(1L, input.inputSnapshotId);
        metadata.optimizerType = OPTIMIZER_TYPE;
        metadata.startedAtNanos = started;
        metadata.inputSnapshotId = input.inputSnapshotId;
        metadata.marketDataSnapshotSeq = input.marketDataSnapshotSeq;
        metadata.venueStatsSnapshotSeq = input.venueStatsSnapshotSeq;
        metadata.modelSignalVersion = input.modelSignalVersion;
        metadata.currentPolicyVersion = input.currentPolicyVersion;
        return metadata;
    }

    private void lifecycle(final int type, final String message) {
        if (lifecycleEvents == null) {
            return;
        }
        lifecycleEvents.append(new LifecycleEvent(nextLifecycleEventId++, Math.max(1L, System.nanoTime()),
                OPTIMIZER_TYPE, type, 0L, message));
    }
}
