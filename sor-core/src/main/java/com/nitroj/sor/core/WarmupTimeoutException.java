package com.nitroj.sor.core;

/**
 * Responsibility: reports that engine warmup did not complete within the
 * configured budget.
 *
 * <p>Role in system: P8-06 makes warmup a framework readiness contract; this
 * exception keeps readiness failures explicit.</p>
 *
 * <p>Relationships: thrown by {@link SorEngineImpl#warmup(int)}.</p>
 *
 * <p>Lifecycle: allocated only on control-plane failure paths.</p>
 *
 * <p>Design intent: separate warmup failure from ordinary illegal state
 * submission errors.</p>
 */
public final class WarmupTimeoutException extends RuntimeException {
    public WarmupTimeoutException(final String message) {
        super(message);
    }
}
