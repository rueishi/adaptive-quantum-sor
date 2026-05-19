package com.nitroj.adaptive.quantum.sor.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link InstrumentMetadata}.
 *
 * <p>Role in system: verifies dense instrument metadata lookup and validation.</p>
 *
 * <p>Relationships: independent from venue metadata and risk snapshots.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: metadata constructors validate static config before engine
 * startup proceeds.</p>
 */
final class InstrumentMetadataTest {
    @Test
    void simulatedMetadataAndAccessorsWork() {
        final InstrumentMetadata instruments = InstrumentMetadata.simulated(2);

        assertEquals("INST1", instruments.symbol(1));
        assertTrue(instruments.isEnabled(0));
        assertEquals(2, instruments.instrumentCount());
    }

    @Test
    void invalidInstrumentMetadataFails() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> InstrumentMetadata.simulated(0)).getMessage().contains("instrumentCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new InstrumentMetadata(new String[0], new boolean[0])).getMessage().contains("symbols"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new InstrumentMetadata(new String[]{" "}, new boolean[]{true})).getMessage().contains("symbol"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> InstrumentMetadata.simulated(1).symbol(1)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> InstrumentMetadata.simulated(1).isEnabled(1)).getMessage().contains("instrumentId"));
    }
}
