package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 7 candidate-set boundary contracts.
 */
final class PolicyCandidateSetTest {
    @Test
    void candidateSetRejectsDuplicateCanonicalHashes() {
        final var policy = TestPolicyFixtures.policy();
        final var duplicate = List.of(
                new PolicyCandidate(0, "base", null, policy),
                new PolicyCandidate(1, "duplicate", null, policy)
        );

        assertEquals("candidate policy hashes must be distinct",
                assertThrows(IllegalArgumentException.class, () -> new PolicyCandidateSet(duplicate)).getMessage());
    }

    @Test
    void candidateSetRequiresDeterministicOrdinalIds() {
        final var policy = TestPolicyFixtures.policy();
        final var nonOrdinal = List.of(new PolicyCandidate(1, "bad-ordinal", null, policy));

        assertEquals("candidate IDs must be deterministic ordinals starting at 0",
                assertThrows(IllegalArgumentException.class, () -> new PolicyCandidateSet(nonOrdinal)).getMessage());
    }

    @Test
    void singleCandidateSetIsExplicitFallbackShape() {
        final var candidate = new PolicyCandidate(0, "single", null, TestPolicyFixtures.policy());
        final PolicyCandidateSet set = new PolicyCandidateSet(List.of(candidate));

        assertFalse(set.robustSelectionReady());
        assertSame(candidate, set.singleCandidate());
    }

    @Test
    void robustConfigDefaultsToDisabledCvarKAndValidatesMinMaxOptIn() {
        final RobustSelectionConfig defaults = RobustSelectionConfig.defaults();

        assertFalse(defaults.enabled());
        assertEquals(RobustObjectiveType.CVAR_K, defaults.objective());
        assertEquals(10, defaults.cvarKPercent());
        assertThrows(IllegalArgumentException.class, () -> new RobustSelectionConfig(
                true,
                RobustObjectiveType.MIN_MAX,
                10,
                false,
                "s",
                1L,
                List.of("robust"),
                RobustSelectionConfig.CandidateGrid.defaults(),
                RobustSelectionConfig.Adequacy.defaults(),
                null
        ));
    }
}
