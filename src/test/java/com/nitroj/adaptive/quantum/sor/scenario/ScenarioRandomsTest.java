package com.nitroj.adaptive.quantum.sor.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Responsibility: verify independent deterministic scenario random streams.
 *
 * <p>Role in system: covers the P5-TC-001 requirement that market, order,
 * venue, and session randomness cannot perturb each other accidentally.</p>
 *
 * <p>Relationships: tests {@link ScenarioRandoms} directly.</p>
 *
 * <p>Lifecycle: executed by scenario unit tests.</p>
 *
 * <p>Design intent: same seed must replay exactly while named streams remain
 * independent.</p>
 */
final class ScenarioRandomsTest {
    @Test
    void sameSeedCreatesRepeatableIndependentStreams() {
        final ScenarioRandoms first = new ScenarioRandoms(99L);
        final ScenarioRandoms second = new ScenarioRandoms(99L);
        final ScenarioRandoms perturbed = new ScenarioRandoms(99L);

        assertEquals(first.nextMarketInt(10_000), second.nextMarketInt(10_000));
        assertEquals(first.nextOrderInt(10_000), second.nextOrderInt(10_000));
        assertEquals(first.nextVenueInt(10_000), second.nextVenueInt(10_000));
        assertEquals(first.nextSessionInt(10_000), second.nextSessionInt(10_000));

        perturbed.nextOrderInt(10_000);
        assertEquals(new ScenarioRandoms(99L).nextMarketInt(10_000), perturbed.nextMarketInt(10_000));
        assertNotEquals(new ScenarioRandoms(100L).nextMarketInt(10_000), new ScenarioRandoms(99L).nextMarketInt(10_000));
    }
}
