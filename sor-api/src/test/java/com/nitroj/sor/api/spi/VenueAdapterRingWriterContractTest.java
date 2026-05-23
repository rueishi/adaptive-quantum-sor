package com.nitroj.sor.api.spi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies venue adapter ring-writer semantics.
 *
 * <p>Role in system: P8-04 defines false-return backpressure instead of
 * blocking or throwing on full venue rings.</p>
 *
 * <p>Relationships: covers {@link VenueAdapter}, {@link RingWriter}, and
 * {@link ChildOrderRef}.</p>
 *
 * <p>Lifecycle: uses an in-memory writer with capacity one.</p>
 *
 * <p>Design intent: keep venue handoff contract simple and hot-path safe.</p>
 */
class VenueAdapterRingWriterContractTest {
    /**
     * Verifies a writer returns true while capacity exists and false once full.
     */
    @Test
    void ringWriterReturnsFalseOnFull() {
        final CapacityOneWriter writer = new CapacityOneWriter();
        final ChildOrderRef child = new ChildOrderRef().set(1, 2, 3, 1, 100, 50, 9);

        assertTrue(writer.offer(child));
        assertFalse(writer.offer(child));
    }

    private static final class CapacityOneWriter implements RingWriter {
        private boolean full;

        @Override
        public boolean offer(final ChildOrderRef childOrder) {
            if (full) {
                return false;
            }
            full = true;
            return true;
        }
    }
}
