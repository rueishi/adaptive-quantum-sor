package com.nitroj.sor.core.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link MarketSessionState}.
 *
 * <p>Role in system: verifies per-instrument session open/closed state.</p>
 *
 * <p>Relationships: isolated session-state test.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: both open and closed values are explicit edge cases for
 * future routing guards.</p>
 */
final class MarketSessionStateTest {
    @Test
    void setOpenAndIsOpenWork() {
        final MarketSessionState market = new MarketSessionState(1);

        assertFalse(market.isOpen(0));
        market.setOpen(0, true);
        assertTrue(market.isOpen(0));
        market.setOpen(0, false);
        assertFalse(market.isOpen(0));
    }

    @Test
    void invalidInstrumentFails() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MarketSessionState(0)).getMessage().contains("instrumentCount"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> new MarketSessionState(1).isOpen(1)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> new MarketSessionState(1).setOpen(1, true)).getMessage().contains("instrumentId"));
    }
}
