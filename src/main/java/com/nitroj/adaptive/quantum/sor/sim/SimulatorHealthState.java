package com.nitroj.adaptive.quantum.sor.sim;

/**
 * Responsibility: retain the current health of the Phase 1 simulator runtime.
 *
 * <p>Role in system: simulator supervisors mark this state failed when a
 * guarded simulator task throws unexpectedly. Optimizer and publication guards
 * can then avoid publishing policy updates derived from partial or corrupt
 * simulator state.</p>
 *
 * <p>Relationships: written by {@link SimulatorSupervisor} and read by
 * {@link SimulatorPublicationGuard}. Lifecycle logging stores the human-readable
 * failure event separately.</p>
 *
 * <p>Lifecycle: allocated with the simulator graph or test harness and retained
 * until an explicit recovery/restart creates a new healthy instance.</p>
 *
 * <p>Design intent: keep failure state primitive and observable so Phase 1 can
 * prove failsafe behavior without introducing a scheduler or background engine.</p>
 */
public final class SimulatorHealthState {
    private boolean healthy = true;
    private int failureCount;
    private String failedSimulatorName = "";
    private String failureMessage = "";
    private long failedAtNanos = -1L;

    /**
     * Records a simulator failure and makes publication guards reject dependent
     * optimizer output until recovery creates or resets health state.
     *
     * @param simulatorName logical simulator/component name
     * @param message failure detail, usually the thrown exception message
     * @param timestampNanos simulated or monotonic timestamp for the failure
     */
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

    /**
     * Resets health after an explicit simulator recovery/restart.
     */
    public void markHealthy() {
        healthy = true;
        failedSimulatorName = "";
        failureMessage = "";
        failedAtNanos = -1L;
    }

    public boolean healthy() {
        return healthy;
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
