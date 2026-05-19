package com.nitroj.adaptive.quantum.sor.audit;

import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link RouteAuditEvent}.
 *
 * <p>Role in system: verifies compact route audit fields and validation before
 * audit buffering is introduced.</p>
 *
 * <p>Relationships: uses order status constants but otherwise tests the audit
 * carrier in isolation.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: primitive audit fields are asserted directly because they
 * are the public compact event contract.</p>
 */
final class RouteAuditEventTest {
    @Test
    void setPopulatesFields() {
        final RouteAuditEvent event = new RouteAuditEvent();

        event.set(1, 20, 30, 40, 50, 2, 10, OrderStatus.NEW);

        assertEquals(1, event.eventId);
        assertEquals(20, event.timestampNanos);
        assertEquals(30, event.parentOrderId);
        assertEquals(40, event.policyVersion);
        assertEquals(50, event.policyHash64);
        assertEquals(2, event.childOrderCount);
        assertEquals(10, event.residualQty);
        assertEquals(OrderStatus.NEW, event.status);
    }

    @Test
    void setRejectsInvalidFields() {
        final RouteAuditEvent event = new RouteAuditEvent();

        assertTrue(assertThrows(IllegalArgumentException.class, () -> event.set(0, 0, 1, 1, 1, 0, 0, OrderStatus.NEW)).getMessage().contains("eventId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> event.set(1, 0, 0, 1, 1, 0, 0, OrderStatus.NEW)).getMessage().contains("parentOrderId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> event.set(1, 0, 1, 1, 1, -1, 0, OrderStatus.NEW)).getMessage().contains("childOrderCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> event.set(1, 0, 1, 1, 1, 0, -1, OrderStatus.NEW)).getMessage().contains("residualQty"));
    }
}
