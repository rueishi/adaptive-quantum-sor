package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies public API boundary separation.
 *
 * <p>Role in system: keeps hot-path order submission on {@link SorEngine} and
 * reset/diagnostics on {@link SorControlPlane}.</p>
 *
 * <p>Relationships: reflection-level guard for API review.</p>
 *
 * <p>Lifecycle: run by `:sor-api:test` for public API changes.</p>
 *
 * <p>Design intent: make future API drift visible in tests.</p>
 */
class SorApiBoundaryTest {
    @Test
    void sorEngineDoesNotExposeResetOrDiagnosticMethods() {
        final Set<String> methods = methodNames(SorEngine.class);

        assertTrue(methods.contains("submitParentOrder"));
        assertTrue(methods.contains("cancelParentOrder"));
        assertFalse(methods.contains("reset"));
        assertFalse(methods.contains("stateSummary"));
        assertFalse(methods.contains("marketDataSnapshot"));
    }

    @Test
    void controlPlaneOwnsResetAndDiagnostics() {
        final Set<String> methods = methodNames(SorControlPlane.class);

        assertEquals(Set.of("reset", "stateSummary", "marketDataSnapshot"), methods);
    }

    private static Set<String> methodNames(final Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
