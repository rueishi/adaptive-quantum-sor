package com.nitroj.adaptive.quantum.sor.policy.compile;

import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

/**
 * Responsibility: define policy compilation from optimizer output.
 *
 * <p>Role in system: the compiler turns mutable warm-path optimizer structures
 * into immutable execution/audit policy artifacts.</p>
 *
 * <p>Relationships: implemented by {@link DefaultPolicyCompiler}; outputs are
 * validated and published by later policy components.</p>
 *
 * <p>Lifecycle: called after lint and validation input is ready, before
 * publication.</p>
 *
 * <p>Design intent: separate deterministic artifact construction from linting,
 * validation, and publication gates.</p>
 */
public interface PolicyCompiler {
    /**
     * Compiles a candidate and optimizer outputs into a policy.
     */
    SorPolicy compile(
            MutablePolicyCandidate candidate,
            StrategicVenueSubsetResult strategicResult,
            TacticalPolicyResult tacticalResult
    );
}
