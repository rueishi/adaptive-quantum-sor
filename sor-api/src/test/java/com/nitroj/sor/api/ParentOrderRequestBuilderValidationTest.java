package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verifies API-boundary validation for parent order requests.
 *
 * <p>Role in system: malformed integrator input must fail before reaching the
 * engine implementation or hot routing path.</p>
 *
 * <p>Relationships: covers the immutable {@link ParentOrderRequest} record and
 * its builder.</p>
 *
 * <p>Lifecycle: constructs DTOs directly in unit tests.</p>
 *
 * <p>Design intent: keep request validation deterministic, cheap, and explicit
 * for integrators.</p>
 */
class ParentOrderRequestBuilderValidationTest {
    /**
     * Verifies a valid builder sequence produces the expected immutable DTO.
     */
    @Test
    void builderCreatesValidRequest() {
        final ParentOrderRequest request = ParentOrderRequest.builder()
                .instrumentId(7)
                .side(Side.BUY)
                .quantity(100)
                .urgency(2)
                .clientOrderId(99)
                .arrivalEpochNanos(1234)
                .build();

        assertEquals(7, request.instrumentId());
        assertEquals(Side.BUY, request.side());
        assertEquals(100, request.quantity());
        assertEquals(2, request.urgencyId());
        assertEquals(99, request.clientOrderId());
        assertEquals(1234, request.arrivalEpochNanos());
    }

    /**
     * Verifies invalid field values fail with clear messages.
     */
    @Test
    void builderRejectsInvalidInputs() {
        assertMessage("instrumentId must be non-negative", () -> validBuilder().instrumentId(-1).build());
        assertMessage("side must be Side.BUY or Side.SELL", () -> validBuilder().side(99).build());
        assertMessage("quantity must be positive", () -> validBuilder().quantity(0).build());
        assertMessage("urgencyId must be non-negative", () -> validBuilder().urgency(-1).build());
        assertMessage("arrivalEpochNanos must be non-negative", () -> validBuilder().arrivalEpochNanos(-1).build());
    }

    /**
     * Creates a valid baseline builder for negative tests.
     */
    private static ParentOrderRequest.Builder validBuilder() {
        return ParentOrderRequest.builder()
                .instrumentId(1)
                .side(Side.SELL)
                .quantity(1)
                .urgency(0);
    }

    /**
     * Asserts an invalid build path throws the expected message.
     */
    private static void assertMessage(final String message, final ThrowingRunnable runnable) {
        assertEquals(message, assertThrows(IllegalArgumentException.class, runnable::run).getMessage());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
