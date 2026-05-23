package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Responsibility: verifies that the Phase 8 test JVM is running on the JDK
 * level required by the productionization framework.
 *
 * <p>Role in system: this is the first boot-level guard for P8-01. It proves
 * that Gradle selected a JDK 25 toolchain before any framework modules are
 * introduced in P8-02.</p>
 *
 * <p>Relationships: complements {@link GcConfigurationTest}, which checks the
 * garbage collector selected for the same test JVM.</p>
 *
 * <p>Lifecycle: executed by the normal JUnit test task during every
 * verification run.</p>
 *
 * <p>Design intent: fail quickly with a precise version mismatch rather than
 * letting later Phase 8 cards produce noisy compiler or benchmark failures.</p>
 */
class BuildToolchainVerificationTest {
    /**
     * Confirms the active JVM reports the Java 25 specification version.
     */
    @Test
    void testJvmUsesJava25Specification() {
        assertEquals("25", System.getProperty("java.specification.version"));
    }
}
