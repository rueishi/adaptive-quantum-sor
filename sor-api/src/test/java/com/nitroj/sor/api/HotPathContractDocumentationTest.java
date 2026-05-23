package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies public API methods carry the Phase 8 hot-path or
 * control-plane contract language in source documentation.
 *
 * <p>Role in system: the hot-path contract is part of the API, so this test
 * prevents undocumented public method additions.</p>
 *
 * <p>Relationships: scans source files for the classes introduced by P8-03.
 * P8-04 extends the same idea to SPI interfaces.</p>
 *
 * <p>Lifecycle: runs as a normal source-level unit test in `sor-api`.</p>
 *
 * <p>Design intent: keep performance expectations visible to integrators in
 * JavaDoc, not only in the phase specification.</p>
 */
class HotPathContractDocumentationTest {
    private static final Path API_SOURCE = Path.of("sor-api/src/main/java/com/nitroj/sor/api");
    private static final List<Class<?>> DOCUMENTED_TYPES = List.of(
            SorEngine.class,
            SorEngineBuilder.class,
            SorEventListener.class,
            Registration.class,
            ParentOrderRequest.Builder.class
    );

    /**
     * Confirms each public method in the P8-03 API source has hot-path or
     * control-plane contract language nearby.
     *
     * @throws IOException if source files cannot be read
     */
    @Test
    void publicMethodsDocumentHotPathOrControlPlaneContract() throws IOException {
        for (Class<?> type : DOCUMENTED_TYPES) {
            final String source = Files.readString(sourcePath(type));
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                    assertTrue(hasContractLanguage(source, method.getName()),
                            () -> type.getName() + "#" + method.getName() + " lacks contract documentation");
                }
            }
        }
    }

    /**
     * Resolves nested classes back to their top-level Java source file.
     */
    private static Path sourcePath(final Class<?> type) {
        Class<?> topLevel = type;
        while (topLevel.getEnclosingClass() != null) {
            topLevel = topLevel.getEnclosingClass();
        }
        return API_SOURCE.resolve(topLevel.getSimpleName() + ".java");
    }

    /**
     * Looks at the source chunk before a method and searches for the contract
     * phrases required by the Phase 8 specification.
     */
    private static boolean hasContractLanguage(final String source, final String methodName) {
        int index = source.indexOf(" " + methodName + "(");
        if (index < 0) {
            index = source.indexOf(" " + methodName + "(final");
        }
        if (index < 0) {
            return false;
        }
        final int start = Math.max(0, index - 700);
        final String nearby = source.substring(start, index);
        return nearby.contains("Hot-path method")
                || nearby.contains("Control-plane method")
                || nearby.contains("Control-plane callback")
                || nearby.contains("control-plane callback")
                || nearby.contains("control-plane object");
    }
}
