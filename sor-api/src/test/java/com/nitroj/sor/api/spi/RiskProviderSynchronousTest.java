package com.nitroj.sor.api.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies risk providers populate decisions synchronously.
 *
 * <p>Role in system: risk is on the routing path, so the SPI must be a direct
 * method call with caller-owned output.</p>
 *
 * <p>Relationships: covers {@link RiskProvider}, {@link RiskCheckRequest}, and
 * {@link RiskDecision}.</p>
 *
 * <p>Lifecycle: invokes a trivial provider in-process.</p>
 *
 * <p>Design intent: deter RPC-like risk implementations from the hot path.</p>
 */
class RiskProviderSynchronousTest {
    /**
     * Confirms a provider sets the decision before returning.
     */
    @Test
    void checkPopulatesDecisionBeforeReturn() {
        final RiskProvider provider = (request, decision) -> decision.allow();
        final RiskDecision decision = new RiskDecision();

        provider.check(new RiskCheckRequest().set(1, 2, 1, 100), decision);

        assertTrue(decision.allowed());
    }
}
