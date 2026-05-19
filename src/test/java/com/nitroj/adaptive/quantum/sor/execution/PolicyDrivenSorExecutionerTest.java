package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderStatus;
import com.nitroj.adaptive.quantum.sor.model.VenueStatus;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify adaptive policy-driven routing.
 *
 * <p>Role in system: covers P1-TC-017 route-list walking, live liquidity,
 * max-child limits, policy identity stamping, audit output, and fail-safe cases.</p>
 *
 * <p>Relationships: uses published policy fixtures and production market,
 * session, risk, and child-buffer structures.</p>
 *
 * <p>Lifecycle: executed by Gradle as adaptive execution coverage.</p>
 *
 * <p>Design intent: assert execution consumes policy only once at decision
 * start through the publisher active reference.</p>
 */
final class PolicyDrivenSorExecutionerTest {
    @Test
    void validParentRoutesAndStampsPolicyIdentity() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final ChildOrderBuffer output = new ChildOrderBuffer(4);

        final RouteDecisionResult result = executioner(fixture).route(ExecutionFixtures.buy(600), output);

        assertEquals(1, result.childOrderCount);
        assertEquals(0, result.residualQty);
        assertEquals(fixture.policy().policyVersion, output.get(0).policyVersion);
        assertEquals(fixture.policy().policyHash64, output.get(0).policyHash64);
        assertEquals(result.policyVersion, result.auditEvent.policyVersion);
    }

    @Test
    void noActivePolicyRejects() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final PolicyPublisher empty = new PolicyPublisher(PublicationGate.permissive(), new com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore());

        final RouteDecisionResult result = new PolicyDrivenSorExecutioner(empty, fixture.market(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), new ChildOrderBuffer(2));

        assertEquals(OrderStatus.NO_ACTIVE_POLICY, result.status);
        assertEquals(100, result.residualQty);
    }

    @Test
    void noLiquidityAndPartialLiquidityRecordResidual() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        fixture.market().updateTopOfBook(0, 0, 100, 101, 1_000, 0);
        fixture.market().updateTopOfBook(0, 1, 100, 102, 1_000, 0);
        final PolicyDrivenSorExecutioner executioner = executioner(fixture);

        final RouteDecisionResult none = executioner.route(ExecutionFixtures.buy(100), new ChildOrderBuffer(2));
        fixture.market().updateTopOfBook(0, 0, 100, 101, 1_000, 50);
        final RouteDecisionResult partial = executioner.route(ExecutionFixtures.buy(100), new ChildOrderBuffer(2));

        assertEquals(OrderStatus.NO_LIQUIDITY, none.status);
        assertEquals(100, none.residualQty);
        assertEquals(50, partial.routedQty);
        assertEquals(50, partial.residualQty);
    }

    @Test
    void maxChildQtyRespectedAndDisabledVenueSkipped() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        fixture.sessions().setStatus(0, VenueStatus.DISABLED);
        final ChildOrderBuffer output = new ChildOrderBuffer(4);

        final RouteDecisionResult result = executioner(fixture).route(ExecutionFixtures.buy(800), output);

        assertTrue(output.get(0).venueId != 0);
        assertTrue(output.get(0).quantity <= fixture.risk().maxChildQty(0));
        assertTrue(result.residualQty >= 0);
    }

    private static PolicyDrivenSorExecutioner executioner(final ExecutionFixtures.Fixture fixture) {
        return new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk());
    }
}
