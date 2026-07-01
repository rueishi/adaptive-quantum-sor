package com.nitroj.sor.core.recovery;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;
import com.nitroj.sor.core.policy.PolicyPublisher;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.publication.PublicationGateResult;
import com.nitroj.sor.core.policy.validation.PolicyValidationReport;

import java.util.List;
import java.util.Optional;

/**
 * Responsibility: coordinate Phase 1 restart into a known safe state.
 *
 * <p>Role in system: startup recovery owns the control-plane decision to use a
 * valid in-memory snapshot or regenerate a baseline policy before routing can
 * proceed.</p>
 *
 * <p>Relationships: calls {@link InitialPolicyBootstrap}, publishes through
 * {@link PolicyPublisher}, and records recovery milestones as lifecycle
 * events.</p>
 *
 * <p>Lifecycle: created during application startup, invoked after config and
 * metadata dimensions are reloaded, then queried by engine wiring before
 * accepting parent-order routing.</p>
 *
 * <p>Design intent: make startup safety explicit. A bad snapshot is never
 * published; missing or rejected snapshots fall back to a deterministic
 * bootstrap policy where possible.</p>
 */
public final class StartupRecoveryCoordinator {
    private static final int COMPONENT_ID = 26;

    private final PolicyPublisher publisher;
    private final InitialPolicyBootstrap bootstrap;
    private final InMemoryLifecycleEventStore lifecycleEvents;
    private long nextEventId = 1L;

    public StartupRecoveryCoordinator(
            final PolicyPublisher publisher,
            final InitialPolicyBootstrap bootstrap,
            final InMemoryLifecycleEventStore lifecycleEvents
    ) {
        if (publisher == null || bootstrap == null || lifecycleEvents == null) {
            throw new IllegalArgumentException("recovery dependencies must not be null");
        }
        this.publisher = publisher;
        this.bootstrap = bootstrap;
        this.lifecycleEvents = lifecycleEvents;
    }

    /**
     * Restores a valid optional snapshot or regenerates and publishes a baseline
     * policy.
     *
     * <p>The method validates the snapshot against the reloaded config
     * dimensions before publication. If the snapshot is absent or invalid, it
     * attempts baseline bootstrap. If both paths fail, the publisher remains
     * without an active policy and callers must keep routing in safe
     * {@code NO_ACTIVE_POLICY} mode.</p>
     *
     * @param config reloaded startup configuration and metadata dimensions
     * @param snapshot optional in-memory policy snapshot from a demo restart
     * @param nowNanos monotonic timestamp used for publication gating/events
     * @return recovery outcome including whether routing is safe
     */
    public StartupRecoveryResult recover(final SorConfig config, final Optional<SorPolicy> snapshot, final long nowNanos) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot optional must not be null");
        }
        if (nowNanos < 0) {
            throw new IllegalArgumentException("nowNanos must be non-negative");
        }

        if (snapshot.isPresent()) {
            final SorPolicy candidate = snapshot.get();
            if (snapshotMatchesConfig(candidate, config)) {
                final PublicationGateResult result = publish(candidate, nowNanos);
                if (result.publishAllowed) {
                    lifecycle(nowNanos, LifecycleEventType.POLICY_PUBLISHED, candidate.policyVersion,
                            "startup restored policy snapshot");
                    return new StartupRecoveryResult(true, true, false, "snapshot_restored", candidate);
                }
                lifecycle(nowNanos, LifecycleEventType.POLICY_REJECTED, candidate.policyVersion,
                        "startup snapshot rejected by publication gate");
            } else {
                lifecycle(nowNanos, LifecycleEventType.POLICY_REJECTED, candidate.policyVersion,
                        "startup snapshot rejected by config metadata mismatch");
            }
        }

        try {
            final SorPolicy baseline = bootstrap.bootstrap(config);
            final PublicationGateResult result = publish(baseline, nowNanos);
            if (result.publishAllowed) {
                lifecycle(nowNanos, LifecycleEventType.POLICY_PUBLISHED, baseline.policyVersion,
                        "startup bootstrapped baseline policy");
                return new StartupRecoveryResult(true, false, true, "baseline_bootstrapped", baseline);
            }
            lifecycle(nowNanos, LifecycleEventType.POLICY_REJECTED, baseline.policyVersion,
                    "startup baseline rejected by publication gate");
            return new StartupRecoveryResult(false, false, false, "baseline_rejected", null);
        } catch (RuntimeException ex) {
            lifecycle(nowNanos, LifecycleEventType.POLICY_REJECTED, 0L,
                    "startup baseline bootstrap failed: " + ex.getMessage());
            return new StartupRecoveryResult(false, false, false, "bootstrap_failed", null);
        }
    }

    /**
     * Reports whether an active policy is currently available for routing.
     *
     * @return true only after a snapshot or baseline has been successfully
     * published
     */
    public boolean safeToRoute() {
        return publisher.activePolicy() != null;
    }

    /**
     * Exposes the active policy selected by recovery.
     *
     * @return active policy or {@code null} when startup recovery did not publish
     */
    public SorPolicy activePolicy() {
        return publisher.activePolicy();
    }

    private PublicationGateResult publish(final SorPolicy policy, final long nowNanos) {
        return publisher.publish(policy, PolicyValidationReport.validReport(), new PolicyLintReport(List.of()),
                new PolicyDiff(), 0, nowNanos);
    }

    private void lifecycle(final long timestampNanos, final int eventType, final long policyVersion, final String message) {
        final LifecycleEvent event = new LifecycleEvent(nextEventId++, Math.max(0L, timestampNanos), COMPONENT_ID,
                eventType, policyVersion, message);
        event.policyVersion = policyVersion;
        lifecycleEvents.append(event);
    }

    private static boolean snapshotMatchesConfig(final SorPolicy policy, final SorConfig config) {
        return policy != null
                && policy.fullPolicyMatrix.instrumentCount == config.instrumentCount()
                && policy.fullPolicyMatrix.venueCount == config.venueCount()
                && policy.fullPolicyMatrix.regimeCount == config.regimeCount()
                && policy.fullPolicyMatrix.urgencyCount == config.urgencyCount()
                && policy.hotRouteBook.instrumentCount == config.instrumentCount()
                && policy.hotRouteBook.regimeCount == config.regimeCount()
                && policy.hotRouteBook.urgencyCount == config.urgencyCount()
                && policy.hotRouteBook.routeVenueId.length > 0;
    }

    /**
     * Responsibility: summarize the result of one startup recovery attempt.
     *
     * <p>Role in system: application wiring and tests use this immutable value
     * to decide whether parent-order routing may be enabled.</p>
     *
     * <p>Relationships: returned by {@link #recover(SorConfig, Optional, long)}
     * and references the policy that was published when recovery succeeded.</p>
     *
     * <p>Lifecycle: short-lived value object created once per recovery attempt.</p>
     *
     * <p>Design intent: expose safe-state decisions without forcing callers to
     * inspect publisher internals.</p>
     */
    public record StartupRecoveryResult(
            boolean safeToRoute,
            boolean restoredSnapshot,
            boolean bootstrappedBaseline,
            String reason,
            SorPolicy activePolicy
    ) {
        public StartupRecoveryResult {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }
}
