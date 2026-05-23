package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verifies listener registration handles close idempotently.
 *
 * <p>Role in system: protects cleanup behavior for integrators that close
 * registrations from multiple lifecycle paths.</p>
 *
 * <p>Relationships: tests the public {@link Registration} helper returned by
 * future engine implementations.</p>
 *
 * <p>Lifecycle: created by a test, closed repeatedly, and discarded.</p>
 *
 * <p>Design intent: make deregistration robust without requiring caller-side
 * state tracking.</p>
 */
class RegistrationIdempotentCloseTest {
    /**
     * Verifies the close action runs once even if close is called repeatedly.
     */
    @Test
    void closeRunsDeregistrationOnce() {
        final AtomicInteger closes = new AtomicInteger();
        final Registration registration = Registration.of(closes::incrementAndGet);

        registration.close();
        registration.close();
        registration.close();

        assertEquals(1, closes.get());
    }

    /**
     * Verifies null deregistration actions are rejected with a clear message.
     */
    @Test
    void nullCloseActionIsRejected() {
        assertEquals("onClose must not be null",
                assertThrows(NullPointerException.class, () -> Registration.of(null)).getMessage());
    }
}
