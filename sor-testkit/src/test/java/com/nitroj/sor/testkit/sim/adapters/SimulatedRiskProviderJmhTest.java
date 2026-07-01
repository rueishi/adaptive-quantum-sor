package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.adapters.*;
import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.RiskCheckRequest;
import com.nitroj.sor.api.spi.RiskDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies simulated risk provider jmh behavior for simulator SPI adapters.
 *
 * <p>Run with :sor-testkit:test to protect deterministic adapter behavior used by scenarios and local servers.</p>
 */
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
