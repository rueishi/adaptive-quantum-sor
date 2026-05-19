package com.nitroj.adaptive.quantum.sor.nativebridge;

import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;

/**
 * Responsibility: carry native tactical optimizer result and diagnostics.
 *
 * <p>Role in system: Java optimizer wrappers consume this value to decide
 * whether to use CUDA output, fall back, or fail a cycle safely.</p>
 *
 * <p>Relationships: returned by {@link TacticalOptimizerNativeBridge} and
 * converted into {@link TacticalPolicyResult} for the existing policy pipeline.</p>
 *
 * <p>Lifecycle: immutable per native invocation.</p>
 *
 * <p>Design intent: keep status, diagnostics, and output arrays together so
 * failure handling cannot accidentally ignore native health.</p>
 */
public final class TacticalOptimizerNativeOutput {
    private final int statusCode;
    private final TacticalPolicyResult tacticalResult;
    private final String diagnostics;
    private final long nativeRuntimeNanos;

    public TacticalOptimizerNativeOutput(
            final int statusCode,
            final TacticalPolicyResult tacticalResult,
            final String diagnostics,
            final long nativeRuntimeNanos
    ) {
        if (diagnostics == null || diagnostics.isBlank()) {
            throw new IllegalArgumentException("diagnostics must not be blank");
        }
        if (nativeRuntimeNanos < 0) {
            throw new IllegalArgumentException("nativeRuntimeNanos must be non-negative");
        }
        this.statusCode = statusCode;
        this.tacticalResult = tacticalResult;
        this.diagnostics = diagnostics;
        this.nativeRuntimeNanos = nativeRuntimeNanos;
    }

    public int statusCode() {
        return statusCode;
    }

    public TacticalPolicyResult tacticalResult() {
        return tacticalResult;
    }

    public String diagnostics() {
        return diagnostics;
    }

    public long nativeRuntimeNanos() {
        return nativeRuntimeNanos;
    }

    /**
     * Reports whether this output can be consumed by the policy pipeline.
     *
     * @return true only when status is OK and a tactical result is present
     */
    public boolean usable() {
        return TacticalOptimizerNativeStatus.ok(statusCode) && tacticalResult != null;
    }
}
