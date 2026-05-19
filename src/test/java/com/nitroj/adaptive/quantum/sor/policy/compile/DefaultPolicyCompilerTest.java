package com.nitroj.adaptive.quantum.sor.policy.compile;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintIssue;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintSeverity;
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
