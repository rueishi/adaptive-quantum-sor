package com.nitroj.sor.core.metadata;

import com.nitroj.sor.core.model.RouteFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link OrderTypeCapabilityMatrix}.
 *
 * <p>Role in system: verifies route-flag capability checks used by policy lint.</p>
 *
 * <p>Relationships: consumes route flag constants but tests capability matrix
 * behavior in isolation.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: unsupported hidden/midpoint/dark flags must be rejected
 * before policy publication.</p>
 */
final class OrderTypeCapabilityMatrixTest {
    @Test
    void setCapabilitiesAndSupportsFlagsWork() {
        final OrderTypeCapabilityMatrix matrix = new OrderTypeCapabilityMatrix(1);
        matrix.setCapabilities(0, true, true, false, false, true, false);

        assertTrue(matrix.supportsFlags(0, RouteFlags.IOC));
        assertTrue(matrix.supportsFlags(0, RouteFlags.POST_ONLY));
        assertTrue(matrix.supportsFlags(0, RouteFlags.LIT));
        assertFalse(matrix.supportsFlags(0, RouteFlags.HIDDEN));
        assertFalse(matrix.supportsFlags(0, RouteFlags.MIDPOINT));
        assertFalse(matrix.supportsFlags(0, RouteFlags.DARK));
        assertFalse(matrix.supportsFlags(0, RouteFlags.add(RouteFlags.HIDDEN, RouteFlags.MIDPOINT)));
    }

    @Test
    void simulatedCapabilitiesArePermissive() {
        final OrderTypeCapabilityMatrix matrix = OrderTypeCapabilityMatrix.simulated(1);

        assertTrue(matrix.supportsFlags(0, RouteFlags.add(RouteFlags.HIDDEN, RouteFlags.MIDPOINT)));
        assertTrue(matrix.supportsFlags(0, RouteFlags.add(RouteFlags.LIT, RouteFlags.DARK)));
    }

    @Test
    void invalidCapabilityInputsFail() {
        final OrderTypeCapabilityMatrix matrix = new OrderTypeCapabilityMatrix(1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> new OrderTypeCapabilityMatrix(0)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> matrix.setCapabilities(1, true, true, true, true, true, true)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> matrix.supportsFlags(1, RouteFlags.IOC)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> matrix.supportsFlags(0, (short) 64)).getMessage().contains("unknown route bits"));
    }
}
