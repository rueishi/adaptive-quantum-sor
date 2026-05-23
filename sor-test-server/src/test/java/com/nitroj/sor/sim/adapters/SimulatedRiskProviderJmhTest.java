package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedRiskProviderJmhTest {
    @Test
    void checkReusesCallerOwnedDecision() {
        final SimulatedRiskProvider provider = new SimulatedRiskProvider();
        final RiskCheckRequest request = new RiskCheckRequest();
        final RiskDecision decision = new RiskDecision();

        provider.maxQuantity(10);
        provider.check(request.set(1, 1, 1, 9), decision);
        assertTrue(decision.allowed());

        provider.check(request.set(1, 1, 1, 11), decision);
        assertFalse(decision.allowed());
    }
}
