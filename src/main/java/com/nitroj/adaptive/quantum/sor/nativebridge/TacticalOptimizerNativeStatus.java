package com.nitroj.adaptive.quantum.sor.nativebridge;

/**
 * Responsibility: define stable native tactical optimizer status codes.
 *
 * <p>Role in system: Java fallback, health, metrics, and diagnostics code use
 * these codes as the boundary contract for CUDA/cuOpt tactical optimization.</p>
 *
 * <p>Relationships: emitted by {@link TacticalOptimizerNativeBridge} and stored
 * by {@link TacticalOptimizerNativeOutput}.</p>
 *
 * <p>Lifecycle: constants are stable for Phase 2 and mirror the documented C++
 * ABI in {@code cpp/tactical_optimizer_api.h}.</p>
 *
 * <p>Design intent: integers keep JNI/Panama/native mapping simple while Java
 * helpers preserve readable failure handling.</p>
 */
public final class TacticalOptimizerNativeStatus {
    public static final int OK = 0;
    public static final int LIBRARY_MISSING = 1;
    public static final int INVALID_INPUT = 2;
    public static final int GPU_UNAVAILABLE = 3;
    public static final int TIMEOUT = 4;
    public static final int OUTPUT_INVALID = 5;
    public static final int NATIVE_FAILURE = 6;

    private TacticalOptimizerNativeStatus() {
    }

    /**
     * Returns whether the supplied status allows consuming native output.
     *
     * @param status native status code
     * @return true only for {@link #OK}
     */
    public static boolean ok(final int status) {
        return status == OK;
    }

    /**
     * Converts a status code to a stable diagnostic token.
     *
     * @param status native status code
     * @return readable token for logs, tests, and reports
     */
    public static String label(final int status) {
        return switch (status) {
            case OK -> "OK";
            case LIBRARY_MISSING -> "LIBRARY_MISSING";
            case INVALID_INPUT -> "INVALID_INPUT";
            case GPU_UNAVAILABLE -> "GPU_UNAVAILABLE";
            case TIMEOUT -> "TIMEOUT";
            case OUTPUT_INVALID -> "OUTPUT_INVALID";
            case NATIVE_FAILURE -> "NATIVE_FAILURE";
            default -> "UNKNOWN";
        };
    }
}
