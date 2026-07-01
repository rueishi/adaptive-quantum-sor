package com.nitroj.sor.core.policy.robust;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsibility: immutable Phase 7 candidate-set boundary from optimizers to publication.
 */
public record PolicyCandidateSet(List<PolicyCandidate> candidates) {
    public PolicyCandidateSet {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        final Set<Long> hashes = new HashSet<>();
        int expectedId = 0;
        for (PolicyCandidate candidate : candidates) {
            if (candidate == null) {
                throw new IllegalArgumentException("candidate must not be null");
            }
            if (candidate.candidateId() != expectedId++) {
                throw new IllegalArgumentException("candidate IDs must be deterministic ordinals starting at 0");
            }
            if (!hashes.add(candidate.canonicalPolicyHash64())) {
                throw new IllegalArgumentException("candidate policy hashes must be distinct");
            }
        }
        candidates = List.copyOf(candidates);
    }

    public int size() {
        return candidates.size();
    }

    public boolean robustSelectionReady() {
        return candidates.size() >= 2;
    }

    public PolicyCandidate singleCandidate() {
        if (candidates.size() != 1) {
            throw new IllegalStateException("candidate set is not single-candidate fallback");
        }
        return candidates.get(0);
    }
}
