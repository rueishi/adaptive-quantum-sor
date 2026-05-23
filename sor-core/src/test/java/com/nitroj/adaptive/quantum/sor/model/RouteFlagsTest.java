package com.nitroj.adaptive.quantum.sor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link RouteFlags}.
 *
 * <p>Role in system: verifies route flag constants and bit operations shared by
 * metadata capability checks and future policy linting.</p>
 *
 * <p>Relationships: isolated helper test; capability matrix has its own test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: bit masks are correctness-sensitive because combinations
 * are stored compactly in policy arrays.</p>
 */
final class RouteFlagsTest {
    @Test
    void constantsAndBitOperationsWork() {
        assertEquals(1, RouteFlags.IOC);
        assertEquals(2, RouteFlags.POST_ONLY);
        assertEquals(4, RouteFlags.HIDDEN);
        assertEquals(8, RouteFlags.MIDPOINT);
        assertEquals(16, RouteFlags.LIT);
        assertEquals(32, RouteFlags.DARK);

        short flags = 0;
        flags = RouteFlags.add(flags, RouteFlags.IOC);
        flags = RouteFlags.add(flags, RouteFlags.HIDDEN);
        assertTrue(RouteFlags.has(flags, RouteFlags.IOC));
        assertTrue(RouteFlags.has(flags, RouteFlags.HIDDEN));
        assertFalse(RouteFlags.has(flags, RouteFlags.MIDPOINT));
        assertTrue(RouteFlags.isKnownMask(RouteFlags.ALL_KNOWN));
        assertFalse(RouteFlags.isKnownMask((short) 64));
    }
}
