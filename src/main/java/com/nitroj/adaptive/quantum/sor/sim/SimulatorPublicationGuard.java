package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGateResult;

/**
 * Responsibility: block dependent policy publication when simulator health is
 * failed.
 *
 * <p>Role in system: P1-SIM-007 requires simulator failures to prevent
 * optimizer cycles from publishing policies derived from failed simulator
 * inputs. This small guard exposes that decision without coupling the simulator
 * supervisor to a specific optimizer implementation.</p>
 *
 * <p>Relationships: reads {@link SimulatorHealthState} and produces the same
 * {@link PublicationGateResult} shape used by publication gates.</p>
 *
 * <p>Lifecycle: created wherever an optimizer or publication cycle wants to
 * check simulator readiness before publishing.</p>
 *
 * <p>Design intent: deterministic, side-effect-free evaluation keeps tests
 * simple and makes simulator health a clear dependency of publication safety.</p>
 */
public final class SimulatorPublicationGuard {
    private final SimulatorHealthState healthState;

    public SimulatorPublicationGuard(final SimulatorHealthState healthState) {
        if (healthState == null) {
            throw new IllegalArgumentException("healthState must not be null");
        }
        this.healthState = healthState;
    }

    /**
     * Returns an allowed result while simulators are healthy and a rejected
     * result with gate name {@code simulator_failed} after a guarded failure.
     */
    public PublicationGateResult evaluate() {
        if (healthState.healthy()) {
            return PublicationGateResult.allowed(0, 0, 0);
        }
        return PublicationGateResult.rejected("simulator_failed");
    }
}
