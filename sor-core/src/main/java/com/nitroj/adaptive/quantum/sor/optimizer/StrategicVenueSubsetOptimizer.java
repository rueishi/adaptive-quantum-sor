package com.nitroj.adaptive.quantum.sor.optimizer;

import com.nitroj.adaptive.quantum.sor.policy.PolicyOptimizationInput;

/**
 * Responsibility: define the strategic venue-subset optimizer contract.
 *
 * <p>Role in system: strategic optimization decides which venues are eligible
 * for each instrument/regime/urgency route universe before tactical weights and
 * penalties are tuned.</p>
 *
 * <p>Relationships: implemented by {@link IsingCudaQStrategicOptimizerStub} in
 * Phase 1 and later by CUDA-Q/Ising backends.</p>
 *
 * <p>Lifecycle: called by the warm-path optimizer coordinator, never by the
 * CPU SOR execution path.</p>
 *
 * <p>Design intent: keep the optimizer boundary explicit so future native
 * backends can replace the deterministic Java stub without changing callers.</p>
 */
public interface StrategicVenueSubsetOptimizer {
    /**
     * Produces a strategic venue subset from a consistent policy input snapshot.
     */
    StrategicVenueSubsetResult optimize(PolicyOptimizationInput input);
}
