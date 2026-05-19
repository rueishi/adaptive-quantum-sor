package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

import java.util.HexFormat;

/**
 * Responsibility: identify one candidate in a Phase 7 robust-selection set.
 */
public record PolicyCandidate(
        int candidateId,
        String generationLabel,
        MutablePolicyCandidate mutableCandidate,
        SorPolicy compiledPolicy
) {
    public PolicyCandidate {
        if (candidateId < 0) {
            throw new IllegalArgumentException("candidateId must be non-negative");
        }
        if (generationLabel == null || generationLabel.isBlank()) {
            throw new IllegalArgumentException("generationLabel must not be blank");
        }
        if (compiledPolicy == null) {
            throw new IllegalArgumentException("compiledPolicy must not be null");
        }
    }

    public long canonicalPolicyHash64() {
        return compiledPolicy.policyHash64;
    }

    public String canonicalPolicyHashSha256Hex() {
        return HexFormat.of().formatHex(compiledPolicy.policyHashSha256);
    }
}
