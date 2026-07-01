package com.nitroj.sor.optnative;

import com.nitroj.sor.core.optimizer.CudaTacticalOptimizerStub;
import com.nitroj.sor.core.optimizer.TacticalPolicyResult;

/**
 * Responsibility: represent the Java-to-native tactical optimizer bridge.
 *
 * <p>Role in system: Phase 2 code calls this boundary instead of directly
 * invoking CUDA/cuOpt. Tests can select deterministic backend modes to verify
 * library loading, native echo, error codes, timeouts, and invalid outputs.</p>
 *
 * <p>Relationships: consumes {@link TacticalOptimizerNativeInput}, returns
 * {@link TacticalOptimizerNativeOutput}, and currently uses the same primitive
 * tactical array contract as the Phase 1 Java stub.</p>
 *
 * <p>Lifecycle: created during optimizer startup, loaded once, and reused per
 * tactical optimization cycle.</p>
 *
 * <p>Design intent: keep the JVM pipeline backend-agnostic while preserving a
 * deterministic no-hardware test backend for CI and local development.</p>
 */
public final class TacticalOptimizerNativeBridge {
    public enum BackendMode {
        AVAILABLE,
        MISSING_LIBRARY,
        GPU_UNAVAILABLE,
        TIMEOUT,
        INVALID_OUTPUT_VALUES,
        NATIVE_FAILURE
    }

    private final BackendMode mode;
    private boolean loaded;

    public TacticalOptimizerNativeBridge() {
        this(BackendMode.AVAILABLE);
    }

    public TacticalOptimizerNativeBridge(final BackendMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        this.mode = mode;
    }

    public static TacticalOptimizerNativeBridge missingLibrary() {
        return new TacticalOptimizerNativeBridge(BackendMode.MISSING_LIBRARY);
    }

    public static TacticalOptimizerNativeBridge gpuUnavailable() {
        return new TacticalOptimizerNativeBridge(BackendMode.GPU_UNAVAILABLE);
    }

    public static TacticalOptimizerNativeBridge timeout() {
        return new TacticalOptimizerNativeBridge(BackendMode.TIMEOUT);
    }

    public static TacticalOptimizerNativeBridge invalidOutputValues() {
        return new TacticalOptimizerNativeBridge(BackendMode.INVALID_OUTPUT_VALUES);
    }

    /**
     * Loads or probes the configured native backend.
     *
     * @return true when native calls can be attempted
     */
    public boolean load() {
        loaded = mode != BackendMode.MISSING_LIBRARY;
        return loaded;
    }

    /**
     * Performs a simple native echo/probe call.
     *
     * @param value value to echo
     * @return echoed value when backend is loaded
     */
    public int echo(final int value) {
        if (!loaded && !load()) {
            throw new IllegalStateException("native library missing");
        }
        if (mode == BackendMode.NATIVE_FAILURE) {
            throw new IllegalStateException("native bridge failure");
        }
        return value;
    }

    /**
     * Runs tactical optimization through the configured native backend mode.
     *
     * @param request validated native input request
     * @return native output status, diagnostics, timings, and tactical arrays
     */
    public TacticalOptimizerNativeOutput optimize(final TacticalOptimizerNativeInput request) {
        if (request == null) {
            return new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.INVALID_INPUT, null,
                    "request missing", 0L);
        }
        if (!loaded && !load()) {
            return new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.LIBRARY_MISSING, null,
                    "native tactical optimizer library missing", 0L);
        }
        return switch (mode) {
            case GPU_UNAVAILABLE -> new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.GPU_UNAVAILABLE,
                    null, "CUDA device unavailable", 0L);
            case TIMEOUT -> new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.TIMEOUT,
                    null, "CUDA tactical optimizer timeout", request.timeoutNanos() + 1L);
            case NATIVE_FAILURE -> new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.NATIVE_FAILURE,
                    null, "native tactical optimizer failure", 1L);
            case MISSING_LIBRARY -> new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.LIBRARY_MISSING,
                    null, "native tactical optimizer library missing", 0L);
            case AVAILABLE, INVALID_OUTPUT_VALUES -> optimizeDeterministically(request);
        };
    }

    public BackendMode mode() {
        return mode;
    }

    private TacticalOptimizerNativeOutput optimizeDeterministically(final TacticalOptimizerNativeInput request) {
        try {
            request.toDirectBuffer();
            final long start = System.nanoTime();
            final TacticalPolicyResult result = new CudaTacticalOptimizerStub().optimize(request.subset(), request.input());
            if (mode == BackendMode.INVALID_OUTPUT_VALUES) {
                result.venueWeightBps[0] = -1;
            }
            final long elapsed = Math.max(1L, System.nanoTime() - start);
            return new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.OK, result,
                    "deterministic CUDA tactical backend", elapsed);
        } catch (RuntimeException ex) {
            return new TacticalOptimizerNativeOutput(TacticalOptimizerNativeStatus.INVALID_INPUT, null,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 0L);
        }
    }
}
