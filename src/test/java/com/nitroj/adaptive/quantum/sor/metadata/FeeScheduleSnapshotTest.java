package com.nitroj.adaptive.quantum.sor.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link FeeScheduleSnapshot}.
 *
 * <p>Role in system: verifies maker/taker fee lookup by dense IV key.</p>
 *
 * <p>Relationships: shares dense IDs with metadata but tests fee storage in
 * isolation.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: fees are allowed to be negative to model rebates.</p>
 */
final class FeeScheduleSnapshotTest {
    @Test
    void setAndGetFeesWork() {
        final FeeScheduleSnapshot fees = new FeeScheduleSnapshot(1, 1);
        fees.setFees(0, 0, -1, 2);

        assertEquals(-1, fees.makerFeeTicks(0, 0));
        assertEquals(2, fees.takerFeeTicks(0, 0));
    }

    @Test
    void invalidFeeLookupFails() {
        final FeeScheduleSnapshot fees = new FeeScheduleSnapshot(1, 1);

        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> fees.makerFeeTicks(0, 1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> fees.takerFeeTicks(0, 1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> fees.setFees(0, 1, 0, 0)).getMessage().contains("venueId"));
    }
}
