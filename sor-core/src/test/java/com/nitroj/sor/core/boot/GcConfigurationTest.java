package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the Phase 8 test JVM is exercising the Z Garbage
 * Collector selected for production framework validation.
 *
 * <p>Role in system: P8-01 moves the runtime baseline from a generic Java 21
 * VM to Java 25 with ZGC. This test makes that GC choice visible in the normal
 * Gradle test output.</p>
 *
 * <p>Relationships: reads JVM management beans only; it does not depend on SOR
 * runtime classes and therefore remains stable during the P8-02 module
 * migration.</p>
 *
 * <p>Lifecycle: invoked in each JUnit run after Gradle applies the test
 * `jvmArgs` declared in `build.gradle`.</p>
 *
 * <p>Design intent: catch accidental removal of `-XX:+UseZGC` before benchmark
 * numbers are compared across incompatible garbage collectors.</p>
 */
class GcConfigurationTest {
    /**
     * Asserts at least one garbage collector MXBean identifies a ZGC collector.
     */
    @Test
    void testJvmUsesZgcCollector() {
        final List<String> collectors = ManagementFactory.getGarbageCollectorMXBeans()
                .stream()
                .map(GarbageCollectorMXBean::getName)
                .toList();

        assertTrue(collectors.stream().anyMatch(name -> name.toLowerCase().contains("zgc")),
                () -> "expected a ZGC collector, found " + collectors);
    }
}
