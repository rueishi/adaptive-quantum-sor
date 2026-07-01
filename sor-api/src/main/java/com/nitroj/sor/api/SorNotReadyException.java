package com.nitroj.sor.api;

/**
 * Responsibility: hot-path-safe rejection for parent orders submitted before
 * the engine is ready.
 *
 * <p>Role in system: gives callers a typed reason code instead of a dynamically
 * constructed string when {@link SorEngine#submitParentOrder(ParentOrderRequest)}
 * is rejected by readiness phase.</p>
 *
 * <p>Relationships: thrown by engine implementations and carries
 * {@link SorReadinessPhase} for observability.</p>
 *
 * <p>Lifecycle: allocated only on rejection paths. Messages are derived from
 * enum constants rather than built per call.</p>
 *
 * <p>Design intent: keep not-ready rejection explicit while preserving the
 * existing {@link IllegalStateException} contract used by earlier tests.</p>
 */
public final class SorNotReadyException extends IllegalStateException {
    private final SorReadinessPhase phase;

    /**
     * Creates a rejection for the supplied readiness phase.
     *
     * @param phase current readiness phase
     */
    public SorNotReadyException(final SorReadinessPhase phase) {
        super(messageFor(phase));
        if (phase == null) {
            throw new IllegalArgumentException("phase must not be null");
        }
        this.phase = phase;
    }

    /**
     * Returns the phase that caused the rejection.
     *
     * @return readiness phase
     */
    public SorReadinessPhase phase() {
        return phase;
    }

    private static String messageFor(final SorReadinessPhase phase) {
        if (phase == null) {
            return "engine not ready";
        }
        return switch (phase) {
            case CONSTRUCTED -> "engine not ready: CONSTRUCTED";
            case WARMED -> "engine not ready: WARMED";
            case MARKET_HYDRATING -> "engine not ready: MARKET_HYDRATING";
            case ORDER_RECONCILING -> "engine not ready: ORDER_RECONCILING";
            case READY -> "engine ready";
            case FAILED -> "engine not ready: FAILED";
            case RECOVERY_REQUIRED -> "engine not ready: RECOVERY_REQUIRED";
        };
    }
}
