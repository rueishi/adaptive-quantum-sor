package com.nitroj.sor.testkit.sim.scenario;

/**
 * Responsibility: retain simulator health after guarded task failures.
 *
 * <p>Role in system: folds legacy simulator health into the new simulator
 * cluster controller so publication safety can be evaluated without core
 * lifecycle classes.</p>
 *
 * <p>Relationships: written by {@link SimulatedClusterController} and read by
 * publication guard methods.</p>
 *
 * <p>Lifecycle: allocated per simulator runtime and reset only by explicit
 * recovery calls.</p>
 *
 * <p>Design intent: keep failure state primitive and deterministic for parity
 * with the legacy guard behavior.</p>
 */
public final class SimulatorHealthState {
    private boolean healthy = true;
    private int failureCount;
    private String failedSimulatorName = "";
    private String failureMessage = "";
    private long failedAtNanos = -1L;

    public boolean healthy() {
        return healthy;
    }

    public void markFailed(final String simulatorName, final String message, final long timestampNanos) {
        if (simulatorName == null || simulatorName.isBlank()) {
            throw new IllegalArgumentException("simulatorName must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (timestampNanos < 0) {
            throw new IllegalArgumentException("timestampNanos must be non-negative");
        }
        healthy = false;
        failureCount++;
        failedSimulatorName = simulatorName;
        failureMessage = message;
        failedAtNanos = timestampNanos;
    }

    public void markHealthy() {
        healthy = true;
        failedSimulatorName = "";
        failureMessage = "";
        failedAtNanos = -1L;
    }

    public int failureCount() {
        return failureCount;
    }

    public String failedSimulatorName() {
        return failedSimulatorName;
    }

    public String failureMessage() {
        return failureMessage;
    }

    public long failedAtNanos() {
        return failedAtNanos;
    }
}
