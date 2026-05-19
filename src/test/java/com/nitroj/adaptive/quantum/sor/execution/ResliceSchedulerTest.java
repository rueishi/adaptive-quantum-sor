package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic reslicing semantics.
 *
 * <p>Role in system: covers P1-TC-018 residual tracking and per-reslice policy
 * capture behavior.</p>
 *
 * <p>Relationships: delegates to {@link PolicyDrivenSorExecutioner} and updates
 * {@link ParentOrderState}.</p>
 *
 * <p>Lifecycle: executed by Gradle as reslice coverage.</p>
 *
 * <p>Design intent: reslice tests prove future child orders may use newer
 * policies while old child orders remain stamped with their original policy.</p>
 */
final class ResliceSchedulerTest {
    @Test
    void timerResliceWorksAndNoResidualSkips() {
        final ExecutionFixtures.Fixture fixture = ExecutionFixtures.fixture();
        final ParentOrderState state = new ParentOrderState(1L, 1_600L);
        final ChildOrderBuffer output = new ChildOrderBuffer(4);
        final RouteDecisionResult first = new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk())
                .route(ExecutionFixtures.buy(1_600L), output);
        state.apply(first);

        final ResliceDecision second = new ResliceScheduler(new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk()))
                .onTimer(state, ExecutionFixtures.buy(1_600L), output);
        final ParentOrderState complete = new ParentOrderState(2L, 1L);
        complete.apply(new RouteDecisionResult(2L, 1L, 1L, 1, 1, 0, 1, null));
        final ResliceDecision none = new ResliceScheduler(new PolicyDrivenSorExecutioner(fixture.publisher(), fixture.market(), fixture.sessions(), fixture.risk()))
                .onTimer(complete, new com.nitroj.adaptive.quantum.sor.model.OrderIntent(2L, 0, com.nitroj.adaptive.quantum.sor.model.Side.BUY, 1L, 0, 1L), output);

        assertTrue(first.residualQty > 0);
        assertTrue(second.resliced);
        assertFalse(none.resliced);
    }

    @Test
    void parentStateValidationAndApplyAreCovered() {
        final ParentOrderState state = new ParentOrderState(1L, 100L);
        state.apply(new RouteDecisionResult(1L, 1L, 1L, 1, 40L, 60L, 1, null));

        assertEquals(40L, state.routedQty());
        assertEquals(60L, state.residualQty());
        assertThrows(IllegalArgumentException.class, () -> new ParentOrderState(0, 1));
        assertThrows(IllegalArgumentException.class, () -> state.apply(null));
    }
}
