package com.nitroj.adaptive.quantum.sor.policy.robust;

/**
 * Responsibility: enumerate Phase 7 robust-selection objectives and fallbacks.
 */
public enum RobustObjectiveType {
    EXPECTED,
    MIN_MAX,
    CVAR_K,
    MIN_REGRET,
    SINGLE_CANDIDATE_FALLBACK,
    ADEQUACY_FALLBACK
}
