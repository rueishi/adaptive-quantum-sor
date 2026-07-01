package com.nitroj.sor.core.execution;

import com.nitroj.sor.core.model.ChildOrderBuffer;
import com.nitroj.sor.core.model.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify the static SOR baseline.
 *
 * <p>Role in system: covers P1-TC-019 fee-adjusted ranking, no-liquidity
 * handling, and output compatibility with adaptive routing.</p>
 *
 * <p>Relationships: uses the same child buffer/result shape as
 * {@link PolicyDrivenSorExecutioner}.</p>
 *
 * <p>Lifecycle: executed by Gradle as static baseline coverage.</p>
 *
 * <p>Design intent: keep static baseline deterministic and independent from
 * adaptive policy logic.</p>
 */
final class StaticSorExecutionerTest {
    @Test
    void staticSorRoutesByFeeAdjustedPrice() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final ChildOrderBuffer output = new ChildOrderBuffer(4);

        final RouteDecisionResult result = new StaticSorExecutioner(3, fixture.market(), fixture.fees(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), output);

        assertEquals(1, result.childOrderCount);
        assertEquals(1, output.get(0).venueId);
    }

    @Test
    void staticSorHandlesNoLiquidity() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        for (int venueId = 0; venueId < 3; venueId++) {
            fixture.market().updateTopOfBook(0, venueId, 100, 101 + venueId, 1_000, 0);
        }

        final RouteDecisionResult result = new StaticSorExecutioner(3, fixture.market(), fixture.fees(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), new ChildOrderBuffer(4));

        assertEquals(OrderStatus.NO_LIQUIDITY, result.status);
        assertEquals(100, result.residualQty);
    }

    @Test
    void staticOutputComparableToAdaptiveOutput() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final RouteDecisionResult adaptive = new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), new ChildOrderBuffer(4));
        final RouteDecisionResult statik = new StaticSorExecutioner(3, fixture.market(), fixture.fees(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(100), new ChildOrderBuffer(4));

        assertEquals(adaptive.parentOrderId, statik.parentOrderId);
        assertEquals(adaptive.residualQty, statik.residualQty);
        assertThrows(IllegalArgumentException.class, () -> new StaticSorExecutioner(0, fixture.market(), fixture.fees(), fixture.sessions(), fixture.risk()));
    }
}
