package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies P8-23 risk snapshot and hot-path check parity. */
final class LegacyRiskProviderParityTest {
    @Test
    void riskLimitsUseLegacyDeterministicCaps() {
        final SimRiskLimits snapshot = new SimulatedRiskProvider().generate(new SimConfig(2, 3, 3, 2));

        assertEquals(10_000L, snapshot.maxChildQty(0));
        assertEquals(10_100L, snapshot.maxChildQty(1));
        assertEquals(1_020_000L, snapshot.maxNotional(1, 2));
        assertEquals(2_500, snapshot.maxParticipationBps(1, 2));
    }

    @Test
    void checkRejectsAboveConfiguredQuantity() {
        final SimulatedRiskProvider risk = new SimulatedRiskProvider();
        risk.maxQuantity(100);
        final RiskDecision decision = new RiskDecision();

        risk.check(new RiskCheckRequest().set(1, 1, 0, 101), decision);

        assertFalse(decision.allowed());
        assertThrows(IllegalArgumentException.class, () -> risk.maxQuantity(-1));
        assertThrows(IllegalArgumentException.class, () -> risk.check(null, decision));
    }
}
