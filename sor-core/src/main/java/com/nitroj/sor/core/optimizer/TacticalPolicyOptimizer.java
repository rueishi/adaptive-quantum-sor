package com.nitroj.sor.core.optimizer;

import com.nitroj.sor.core.policy.PolicyOptimizationInput;

/**
 * Responsibility: define the tactical policy optimizer contract.
 *
 * <p>Role in system: tactical optimization tunes weights, penalties, child-size
 * limits, and participation caps inside a strategic venue subset.</p>
 *
 * <p>Relationships: implemented by {@link CudaTacticalOptimizerStub} in Phase 1
 * and later by a native CUDA/cuOpt backend.</p>
 *
 * <p>Lifecycle: called by warm-path optimizer coordination after strategic
 * selection, and never called by the execution hot path.</p>
 *
 * <p>Design intent: isolate tactical numerical tuning from policy compilation
 * so compiler code remains deterministic and backend-agnostic.</p>
 */
public interface TacticalPolicyOptimizer {
    /**
     * Produces bounded tactical arrays for the supplied strategic subset.
     */
    TacticalPolicyResult optimize(StrategicVenueSubsetResult subset, PolicyOptimizationInput input);
}
