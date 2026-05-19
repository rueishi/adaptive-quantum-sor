package com.nitroj.adaptive.quantum.sor.risk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link RiskLimitSnapshot}.
 *
 * <p>Role in system: verifies static risk limit storage and validation before
 * routing and policy linting consume it.</p>
 *
 * <p>Relationships: uses dense instrument/venue IDs through public methods.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: risk limits are control-plane safety checks, so invalid
 * values must fail clearly.</p>
 */
final class RiskLimitSnapshotTest {
    @Test
    void setAndGetRiskLimitsWork() {
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(1, 1);
        risk.setMaxChildQty(0, 1_000);
        risk.setVenueLimits(0, 0, 1_000_000, 2_500);

        assertEquals(1_000, risk.maxChildQty(0));
        assertEquals(1_000_000, risk.maxVenueNotional(0, 0));
        assertEquals(2_500, risk.maxParticipationBps(0, 0));
    }

    @Test
    void invalidRiskLimitsFail() {
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(1, 1);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> risk.setMaxChildQty(0, 0)).getMessage().contains("maxChildQty"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> risk.setMaxChildQty(1, 1)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> risk.setVenueLimits(0, 0, -1, 1)).getMessage().contains("maxVenueNotional"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> risk.setVenueLimits(0, 0, 0, 10_001)).getMessage().contains("maxParticipationBps"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> risk.maxChildQty(1)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> risk.maxVenueNotional(0, 1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> risk.maxParticipationBps(0, 1)).getMessage().contains("venueId"));
    }
}
