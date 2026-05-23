package com.nitroj.sor.optnative;

import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

import java.nio.ByteBuffer;

/**
 * Responsibility: carry Java optimizer state across the native boundary.
 *
 * <p>Role in system: this value validates and exposes only primitive array
 * surfaces required by the CUDA tactical optimizer bridge.</p>
 *
 * <p>Relationships: wraps {@link PolicyOptimizationInput} and
 * {@link StrategicVenueSubsetResult}; {@link TacticalOptimizerBufferLayout}
 * serializes it into native-readable direct buffers.</p>
 *
 * <p>Lifecycle: created per tactical optimizer invocation and discarded after
 * the native backend returns.</p>
 *
 * <p>Design intent: prevent Java object graphs from leaking into the native
 * contract while keeping Phase 2 tests deterministic.</p>
 */
public final class TacticalOptimizerNativeInput {
    private final PolicyOptimizationInput input;
    private final StrategicVenueSubsetResult subset;
    private final long timeoutNanos;

    public TacticalOptimizerNativeInput(
            final PolicyOptimizationInput input,
            final StrategicVenueSubsetResult subset,
            final long timeoutNanos
    ) {
        if (input == null || subset == null) {
            throw new IllegalArgumentException("input and subset must not be null");
        }
        if (timeoutNanos <= 0) {
            throw new IllegalArgumentException("timeoutNanos must be positive");
        }
        final int routeCount = input.instrumentCount * input.regimeCount * input.urgencyCount;
        if (input.instrumentCount <= 0 || input.venueCount <= 0 || input.regimeCount <= 0 || input.urgencyCount <= 0) {
            throw new IllegalArgumentException("input dimensions must be positive");
        }
        if (subset.subsetOffset == null || subset.subsetOffset.length != routeCount + 1) {
            throw new IllegalArgumentException("subsetOffset length must equal routeCount + 1");
        }
        if (subset.selectedVenueIds == null) {
            throw new IllegalArgumentException("selectedVenueIds must not be null");
        }
        this.input = input;
        this.subset = subset;
        this.timeoutNanos = timeoutNanos;
    }

    public PolicyOptimizationInput input() {
        return input;
    }

    public StrategicVenueSubsetResult subset() {
        return subset;
    }

    public long timeoutNanos() {
        return timeoutNanos;
    }

    /**
     * Encodes this request into the documented little-endian direct buffer.
     *
     * @return direct native-readable input buffer
     */
    public ByteBuffer toDirectBuffer() {
        return TacticalOptimizerBufferLayout.writeInput(this);
    }
}
