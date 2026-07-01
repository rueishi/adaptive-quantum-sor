package com.nitroj.sor.optnative;

import com.nitroj.sor.core.optimizer.ising.QuboObjectiveConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Responsibility: expose the Phase 3 CUDA-Q strategic optimizer backend boundary.
 *
 * <p>Role in system: Java strategic optimization code depends on this bridge
 * instead of depending directly on a CUDA-Q implementation. The Adaptive Quantum SOR loads the
 * Gradle-built native backend artifact and uses deterministic exhaustive QUBO
 * solving for small route objectives.</p>
 *
 * <p>Relationships: consumed by {@code CudaQStrategicOptimizer} and tested with
 * the native CMake artifact produced by Gradle.</p>
 *
 * <p>Lifecycle: constructed once per optimizer component and reused across
 * strategic optimizer runs.</p>
 *
 * <p>Design intent: keeping status and selected venues explicit makes invalid
 * native results rejectable before policy publication.</p>
 */
public final class StrategicOptimizerNativeBridge {
    public static final String LIBRARY_BASENAME = "cudaq_strategic_optimizer";

    private final Path libraryPath;
    private final boolean backendAvailable;

    public StrategicOptimizerNativeBridge(final Path nativeLibraryDirectory) {
        if (nativeLibraryDirectory == null) {
            throw new IllegalArgumentException("nativeLibraryDirectory must not be null");
        }
        this.libraryPath = nativeLibraryDirectory.resolve(System.mapLibraryName(LIBRARY_BASENAME));
        this.backendAvailable = Files.isRegularFile(libraryPath);
        if (!backendAvailable) {
            throw new IllegalStateException("strategic optimizer native library missing: " + libraryPath);
        }
    }

    /**
     * Creates an intentionally unavailable bridge for fallback tests.
     */
    public StrategicOptimizerNativeBridge() {
        this.libraryPath = null;
        this.backendAvailable = false;
    }

    public boolean backendAvailable() {
        return backendAvailable;
    }

    public Path libraryPath() {
        return libraryPath;
    }

    /**
     * Solves a small route QUBO and returns native-shaped output.
     */
    public NativeResult optimize(final QuboObjectiveConfig objective) {
        if (!backendAvailable) {
            return NativeResult.failure(StrategicOptimizerNativeStatus.BACKEND_UNAVAILABLE, "backend unavailable");
        }
        if (objective == null) {
            return NativeResult.failure(StrategicOptimizerNativeStatus.INVALID_INPUT, "objective must not be null");
        }
        final int venueCount = objective.venueCount();
        if (venueCount > 30) {
            return NativeResult.failure(StrategicOptimizerNativeStatus.INVALID_INPUT, "venueCount must be <= 30");
        }

        long bestEnergy = Long.MAX_VALUE;
        int bestMask = 0;
        final int maxMask = 1 << venueCount;
        for (int mask = 0; mask < maxMask; mask++) {
            final boolean[] selected = selected(mask, venueCount);
            if (!objective.validSubset(selected)) {
                continue;
            }
            final long energy = objective.energy(selected);
            if (energy < bestEnergy || (energy == bestEnergy && mask < bestMask)) {
                bestEnergy = energy;
                bestMask = mask;
            }
        }
        if (bestEnergy == Long.MAX_VALUE) {
            return NativeResult.failure(StrategicOptimizerNativeStatus.INVALID_RESULT, "no valid strategic subset");
        }
        final short[] selectedVenueIds = selectedVenueIds(bestMask, venueCount);
        return NativeResult.success(selectedVenueIds, bestEnergy);
    }

    public void validate(final NativeResult result, final int venueCount) {
        if (result == null) {
            throw new IllegalArgumentException("native result must not be null");
        }
        if (result.status() != StrategicOptimizerNativeStatus.OK) {
            throw new IllegalStateException("strategic optimizer native status " + result.status() + ": " + result.message());
        }
        if (result.selectedVenueIds().length == 0) {
            throw new IllegalStateException("strategic optimizer native result selected no venues");
        }
        final boolean[] seen = new boolean[venueCount];
        for (final short venueId : result.selectedVenueIds()) {
            if (venueId < 0 || venueId >= venueCount) {
                throw new IllegalStateException("strategic optimizer native result contains invalid venueId " + venueId);
            }
            if (seen[venueId]) {
                throw new IllegalStateException("strategic optimizer native result contains duplicate venueId " + venueId);
            }
            seen[venueId] = true;
        }
    }

    private static boolean[] selected(final int mask, final int venueCount) {
        final boolean[] selected = new boolean[venueCount];
        for (int venueId = 0; venueId < venueCount; venueId++) {
            selected[venueId] = (mask & (1 << venueId)) != 0;
        }
        return selected;
    }

    private static short[] selectedVenueIds(final int mask, final int venueCount) {
        int count = 0;
        for (int venueId = 0; venueId < venueCount; venueId++) {
            if ((mask & (1 << venueId)) != 0) {
                count++;
            }
        }
        final short[] selected = new short[count];
        int write = 0;
        for (int venueId = 0; venueId < venueCount; venueId++) {
            if ((mask & (1 << venueId)) != 0) {
                selected[write++] = (short) venueId;
            }
        }
        return selected;
    }

    public enum StrategicOptimizerNativeStatus {
        OK,
        BACKEND_UNAVAILABLE,
        INVALID_INPUT,
        TIMEOUT,
        INVALID_RESULT,
        NATIVE_FAILURE
    }

    public record NativeResult(
            StrategicOptimizerNativeStatus status,
            short[] selectedVenueIds,
            long objectiveEnergy,
            String message
    ) {
        public NativeResult {
            if (status == null) {
                throw new IllegalArgumentException("status must not be null");
            }
            selectedVenueIds = selectedVenueIds == null ? new short[0] : Arrays.copyOf(selectedVenueIds, selectedVenueIds.length);
            message = message == null ? "" : message;
        }

        @Override
        public short[] selectedVenueIds() {
            return Arrays.copyOf(selectedVenueIds, selectedVenueIds.length);
        }

        public static NativeResult success(final short[] selectedVenueIds, final long objectiveEnergy) {
            return new NativeResult(StrategicOptimizerNativeStatus.OK, selectedVenueIds, objectiveEnergy, "OK");
        }

        public static NativeResult failure(final StrategicOptimizerNativeStatus status, final String message) {
            return new NativeResult(status, new short[0], Long.MAX_VALUE, message);
        }
    }
}
