package com.nitroj.sor.api;

/**
 * Responsibility: reports that the engine cannot accept a hot-path submission
 * because its intake ring is saturated.
 *
 * <p>Role in system: thrown by {@link SorEngine#submitParentOrder} once P8-08
 * replaces the legacy queue with Agrona ring buffers.</p>
 *
 * <p>Relationships: paired with {@link SorEvent.BackpressureRejected} for the
 * asynchronous event stream.</p>
 *
 * <p>Lifecycle: constructed only for rejection paths; successful hot-path
 * submissions must not allocate this exception.</p>
 *
 * <p>Design intent: make expected backpressure explicit without overloading
 * generic illegal-state failures.</p>
 */
public final class BackpressureException extends RuntimeException {
    /**
     * Creates a backpressure exception with a clear operator-facing message.
     *
     * @param message rejection detail
     */
    public BackpressureException(final String message) {
        super(message);
    }
}
