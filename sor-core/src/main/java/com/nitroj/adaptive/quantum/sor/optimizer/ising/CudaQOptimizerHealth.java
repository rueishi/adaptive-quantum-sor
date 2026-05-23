package com.nitroj.adaptive.quantum.sor.optimizer.ising;

import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEvent;
import com.nitroj.adaptive.quantum.sor.lifecycle.LifecycleEventType;

/**
 * Responsibility: record CUDA-Q strategic optimizer availability and failures.
 *
 * <p>Role in system: Phase 3 uses this object to make backend unavailable,
 * timeout, invalid-result, and fallback behavior observable.</p>
 *
 * <p>Relationships: written by fallback handling and exposed to lifecycle/audit
 * tests through the latest event/message fields.</p>
 *
 * <p>Lifecycle: retained for the optimizer component lifetime.</p>
 *
 * <p>Design intent: primitive counters and a lifecycle event are sufficient for
 * the Adaptive Quantum SOR while making fail-safe behavior auditable.</p>
 */
public final class CudaQOptimizerHealth {
    private boolean backendAvailable = true;
    private boolean timedOut;
    private int failureCount;
    private int fallbackCount;
    private String lastMessage = "CUDA-Q strategic optimizer healthy";

    public void recordBackendUnavailable() {
        backendAvailable = false;
        recordFailure("CUDA-Q backend unavailable");
    }

    public void recordTimeout() {
        timedOut = true;
        recordFailure("CUDA-Q strategic optimizer timeout");
    }

    public void recordFailure(final String message) {
        failureCount++;
        lastMessage = message;
    }

    public void recordFallback(final String message) {
        fallbackCount++;
        lastMessage = message;
    }

    public boolean backendAvailable() {
        return backendAvailable;
    }

    public boolean timedOut() {
        return timedOut;
    }

    public int failureCount() {
        return failureCount;
    }

    public int fallbackCount() {
        return fallbackCount;
    }

    public String lastMessage() {
        return lastMessage;
    }

    public LifecycleEvent toLifecycleEvent(final long eventId, final long timestampNanos, final long correlationId) {
        return new LifecycleEvent(
                eventId,
                timestampNanos,
                3,
                LifecycleEventType.POLICY_REJECTED,
                correlationId,
                lastMessage
        );
    }
}
