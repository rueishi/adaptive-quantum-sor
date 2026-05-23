package com.nitroj.sor.optnative;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsibility: load and call the Gradle-built native tactical optimizer library.
 *
 * <p>Role in system: this bridge proves the Java process can cross the real
 * JNI/native boundary produced by the CMake build, rather than only using a
 * deterministic Java simulation.</p>
 *
 * <p>Relationships: invokes native functions implemented in
 * {@code cpp/tactical_optimizer_jni.cpp} and shares status codes with
 * {@link TacticalOptimizerNativeStatus}.</p>
 *
 * <p>Lifecycle: constructed by integration tests or future optimizer wiring
 * after Gradle has built native artifacts. A JVM can load the shared library
 * once; later bridge instances reuse the loaded library.</p>
 *
 * <p>Design intent: keep JNI usage tiny and auditable: echo for liveness and a
 * status-only optimize call for direct-buffer validation.</p>
 */
public final class JniTacticalOptimizerNativeBridge {
    private static final String LIBRARY_NAME = System.mapLibraryName("tactical_optimizer_jni");
    private static String loadedPath;

    public JniTacticalOptimizerNativeBridge(final Path libraryPath) {
        if (libraryPath == null) {
            throw new IllegalArgumentException("libraryPath must not be null");
        }
        if (!Files.isRegularFile(libraryPath)) {
            throw new IllegalStateException("native library not found: " + libraryPath);
        }
        loadOnce(libraryPath.toAbsolutePath().normalize().toString());
    }

    /**
     * Resolves the JNI bridge library from a Gradle native build directory.
     *
     * @param nativeBuildDir build/native directory
     * @return absolute path to the shared library
     */
    public static Path resolveLibrary(final Path nativeBuildDir) {
        if (nativeBuildDir == null) {
            throw new IllegalArgumentException("nativeBuildDir must not be null");
        }
        return nativeBuildDir.resolve(LIBRARY_NAME);
    }

    /**
     * Calls the native echo function through JNI.
     *
     * @param value value to echo
     * @return echoed value
     */
    public int echo(final int value) {
        return echoNative(value);
    }

    /**
     * Calls the native optimize status function with direct buffers.
     *
     * @param input direct native input buffer
     * @param output direct native output buffer
     * @param timeoutNanos timeout budget
     * @return native status code
     */
    public int optimizeStatus(final ByteBuffer input, final ByteBuffer output, final long timeoutNanos) {
        if (input == null || output == null) {
            throw new IllegalArgumentException("input and output buffers must not be null");
        }
        if (!input.isDirect() || !output.isDirect()) {
            throw new IllegalArgumentException("input and output buffers must be direct");
        }
        return optimizeStatusNative(input, output, timeoutNanos);
    }

    private static synchronized void loadOnce(final String libraryPath) {
        if (loadedPath == null) {
            System.load(libraryPath);
            loadedPath = libraryPath;
            return;
        }
        if (!loadedPath.equals(libraryPath)) {
            throw new IllegalStateException("native library already loaded from: " + loadedPath);
        }
    }

    private native int echoNative(int value);

    private native int optimizeStatusNative(ByteBuffer input, ByteBuffer output, long timeoutNanos);
}
