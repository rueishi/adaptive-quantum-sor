package com.nitroj.sor.core.audit;

import com.nitroj.sor.core.execution.ExecutionFixtures;
import com.nitroj.sor.core.execution.PolicyDrivenSorExecutioner;
import com.nitroj.sor.core.model.ChildOrderBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify compact route audit writing.
 *
 * <p>Role in system: covers P1-TC-022 audit append, policy identity, and
 * full-buffer behavior independent of narrative logging.</p>
 *
 * <p>Relationships: writes audit events produced by the policy-driven
 * executioner into {@link InMemoryRouteAuditWriter}.</p>
 *
 * <p>Lifecycle: executed by Gradle for audit writer coverage.</p>
 *
 * <p>Design intent: audit is compact primitive data and does not format
 * lifecycle strings.</p>
 */
final class RouteAuditWriterTest {
    @Test
    void everyRouteEmitsAuditEventWithPolicyIdentity() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final var result = new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), new ChildOrderBuffer(4));
        final RouteAuditEventBuffer buffer = new RouteAuditEventBuffer(4, true);
        final InMemoryRouteAuditWriter writer = new InMemoryRouteAuditWriter(buffer);

        assertTrue(writer.append(result.auditEvent));
        assertEquals(fixture.policy().policyVersion, buffer.snapshot().get(0).policyVersion);
        assertEquals(fixture.policy().policyHash64, buffer.snapshot().get(0).policyHash64);
    }

    @Test
    void auditBufferFullBehaviorFollowsConfig() {
        final RouteAuditEventBuffer strict = new RouteAuditEventBuffer(1, false);
        strict.append(event(1));

        assertFalse(strict.append(event(2)));
        assertEquals(1, strict.droppedCount());

        final RouteAuditEventBuffer dropping = new RouteAuditEventBuffer(1, true);
        dropping.append(event(1));
        dropping.append(event(2));
        assertEquals(2, dropping.snapshot().get(0).eventId);
    }

    @Test
    void auditValidationContractsAreCovered() {
        assertThrows(IllegalArgumentException.class, () -> new RouteAuditEventBuffer(0, true));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryRouteAuditWriter(null));
        assertThrows(IllegalArgumentException.class, () -> new RouteAuditEventBuffer(1, true).append(null));
    }

    private static RouteAuditEvent event(final long id) {
        final RouteAuditEvent event = new RouteAuditEvent();
        event.set(id, id, 1L, 2L, 3L, 1, 0, 1);
        return event;
    }
}
