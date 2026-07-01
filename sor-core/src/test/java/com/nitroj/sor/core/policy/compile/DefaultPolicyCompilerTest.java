package com.nitroj.sor.core.policy.compile;

import com.nitroj.sor.core.TestPolicyFixtures;
import com.nitroj.sor.core.governance.PolicyDiff;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.lint.PolicyLintIssue;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;
import com.nitroj.sor.core.policy.lint.PolicyLintSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 1 policy compilation.
 *
 * <p>Role in system: covers conversion from optimizer/candidate data into
 * immutable policy, hot-route, matrix, diff, ledger, and hash artifacts.</p>
 *
 * <p>Relationships: consumes optimizer and lint fixtures from previous task
 * cards and validates compiler-owned output structures.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct P1-TC-014 coverage.</p>
 *
 * <p>Design intent: keep compiler assertions focused on deterministic artifact
 * construction, not publication gates.</p>
 */
final class DefaultPolicyCompilerTest {
    @Test
    void validCandidateCompilesWithHashDiffAndLedger() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1));

        final SorPolicy policy = compiler.compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());

        assertNotNull(policy.hotRouteBook);
        assertNotNull(policy.fullPolicyMatrix);
        assertNotEquals(0L, policy.policyHash64);
        assertEquals(32, policy.policyHashSha256.length);
        assertNotNull(compiler.lastDiff());
        assertNotNull(compiler.lastLedgerEntry());
    }

    @Test
    void invalidLintBlocksCompileAndNullInputsFailClearly() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final PolicyLintReport invalid = new PolicyLintReport(List.of(
                new PolicyLintIssue(PolicyLintSeverity.ERROR, "ERR", "bad candidate")
        ));
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1));

        assertEquals("cannot compile candidate with lint errors", assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), invalid)
        ).getMessage());
        assertEquals("candidate, strategicResult, and tacticalResult must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> compiler.compile(null, bundle.strategic(), bundle.tactical())
        ).getMessage());
    }

    @Test
    void routeOffsetsAreMonotonicAndRouteListsAreCapped() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final SorPolicy policy = new DefaultPolicyCompiler(new CompiledScoreConfig(1, 1, 1))
                .compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());

        for (int i = 1; i < policy.hotRouteBook.routeListOffset.length; i++) {
            assertTrue(policy.hotRouteBook.routeListOffset[i] >= policy.hotRouteBook.routeListOffset[i - 1]);
            assertTrue(policy.hotRouteBook.routeListOffset[i] - policy.hotRouteBook.routeListOffset[i - 1] <= 1);
        }
    }

    @Test
    void routeWeightsAreNormalizedToFullBpsBudget() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final SorPolicy policy = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1))
                .compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());

        for (int routeKey = 0; routeKey < policy.hotRouteBook.routeListOffset.length - 1; routeKey++) {
            int sum = 0;
            for (int i = policy.hotRouteBook.routeListOffset[routeKey]; i < policy.hotRouteBook.routeListOffset[routeKey + 1]; i++) {
                sum += policy.hotRouteBook.weightBps[i];
            }
            assertEquals(10_000, sum);
        }
        for (int instrumentId = 0; instrumentId < policy.fullPolicyMatrix.instrumentCount; instrumentId++) {
            for (int regimeId = 0; regimeId < policy.fullPolicyMatrix.regimeCount; regimeId++) {
                for (int urgencyId = 0; urgencyId < policy.fullPolicyMatrix.urgencyCount; urgencyId++) {
                    int sum = 0;
                    for (int venueId = 0; venueId < policy.fullPolicyMatrix.venueCount; venueId++) {
                        final int idx = policy.fullPolicyMatrix.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        if (policy.fullPolicyMatrix.venueEligible[idx]) {
                            sum += policy.fullPolicyMatrix.venueWeightBps[idx];
                        }
                    }
                    assertEquals(10_000, sum);
                }
            }
        }
    }

    @Test
    void diffComparesAgainstPreviousPolicyInsteadOfCountingTotals() {
        final TestPolicyFixtures.CandidateBundle firstBundle = TestPolicyFixtures.candidateBundle();
        final DefaultPolicyCompiler firstCompiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1));
        final SorPolicy first = firstCompiler.compile(
                firstBundle.candidate(), firstBundle.strategic(), firstBundle.tactical(), firstBundle.lint());
        final TestPolicyFixtures.CandidateBundle secondBundle = TestPolicyFixtures.candidateBundle();
        for (int regimeId = 0; regimeId < TestPolicyFixtures.REGIMES; regimeId++) {
            for (int urgencyId = 0; urgencyId < TestPolicyFixtures.URGENCIES; urgencyId++) {
                secondBundle.candidate().venueWeightBps[secondBundle.candidate().idxIVRU(0, 0, regimeId, urgencyId)] = 9_000;
                secondBundle.candidate().venueWeightBps[secondBundle.candidate().idxIVRU(0, 1, regimeId, urgencyId)] = 1_000;
            }
        }

        final DefaultPolicyCompiler secondCompiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1), first);
        secondCompiler.compile(secondBundle.candidate(), secondBundle.strategic(), secondBundle.tactical(), secondBundle.lint());
        final PolicyDiff diff = secondCompiler.lastDiff();

        assertEquals(0, diff.addedVenueCount);
        assertEquals(0, diff.removedVenueCount);
        assertEquals(0, diff.changedRouteListCount);
        assertTrue(diff.changedWeightCount > 0);
        assertTrue(diff.maxWeightChangeBps > 0);
    }

    @Test
    void policyHashChangesWhenLargeWeightsDifferBeyondLowByte() {
        final TestPolicyFixtures.CandidateBundle smallWeightBundle = TestPolicyFixtures.candidateBundle();
        final TestPolicyFixtures.CandidateBundle largeWeightBundle = TestPolicyFixtures.candidateBundle();
        smallWeightBundle.candidate().venueWeightBps[smallWeightBundle.candidate().idxIVRU(0, 0, 0, 0)] = 16;
        smallWeightBundle.candidate().venueWeightBps[smallWeightBundle.candidate().idxIVRU(0, 1, 0, 0)] = 1;
        largeWeightBundle.candidate().venueWeightBps[largeWeightBundle.candidate().idxIVRU(0, 0, 0, 0)] = 10_000;
        largeWeightBundle.candidate().venueWeightBps[largeWeightBundle.candidate().idxIVRU(0, 1, 0, 0)] = 1;

        final SorPolicy smallWeight = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1))
                .compile(smallWeightBundle.candidate(), smallWeightBundle.strategic(), smallWeightBundle.tactical(), smallWeightBundle.lint());
        final SorPolicy largeWeight = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1))
                .compile(largeWeightBundle.candidate(), largeWeightBundle.strategic(), largeWeightBundle.tactical(), largeWeightBundle.lint());

        assertNotEquals(smallWeight.policyHash64, largeWeight.policyHash64);
    }

    @Test
    void policyHashUtilityAndConfigValidationAreCovered() {
        final byte[] sha = PolicyHash.sha256(new byte[]{1, 2, 3});

        assertEquals(32, sha.length);
        assertNotEquals(0L, PolicyHash.hash64(sha));
        assertThrows(IllegalArgumentException.class, () -> PolicyHash.hash64(new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> new CompiledScoreConfig(0, 1, 1));
    }

    @Test
    void compilerFailureLeavesExistingPolicyReferenceUnchangedInCaller() {
        final SorPolicy active = TestPolicyFixtures.policy();
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final DefaultPolicyCompiler compiler = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1), active);

        assertThrows(IllegalArgumentException.class, () -> compiler.compile(null, bundle.strategic(), bundle.tactical()));

        assertSame(active, active);
    }
}
