package com.nitroj.adaptive.quantum.sor.policy.lint;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.metadata.OrderTypeCapabilityMatrix;
import com.nitroj.adaptive.quantum.sor.model.RouteFlags;
import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 1 policy lint checks.
 *
 * <p>Role in system: covers candidate acceptance, empty route universes,
 * capability validation, warning handling, and exception-to-report behavior.</p>
 *
 * <p>Relationships: uses optimizer-produced candidate fixtures and metadata
 * capability matrices from earlier task cards.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct P1-TC-012 coverage.</p>
 *
 * <p>Design intent: lint failures are reported, not thrown, so active policy
 * safety can be maintained by later publisher/coordinator cards.</p>
 */
final class DefaultPolicyLintTest {
    @Test
    void validCandidatePasses() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final PolicyLintReport report = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());

        assertFalse(report.hasErrors());
    }

    @Test
    void emptyRouteUniverseFails() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        for (int i = 0; i < bundle.candidate().venueEligible.length; i++) {
            bundle.candidate().venueEligible[i] = false;
        }

        final PolicyLintReport report = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());

        assertTrue(report.hasErrors());
        assertEquals(PolicyLintCode.EMPTY_ROUTE_UNIVERSE, report.issues().get(0).code());
    }

    @Test
    void invalidCapabilityFlagFails() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final OrderTypeCapabilityMatrix capabilities = new OrderTypeCapabilityMatrix(TestPolicyFixtures.VENUES);
        capabilities.setCapabilities(0, true, true, false, false, true, true);
        capabilities.setCapabilities(1, true, true, true, true, true, true);
        capabilities.setCapabilities(2, true, true, true, true, true, true);
        bundle.input().orderTypeCapabilities = capabilities;
        bundle.candidate().routeFlags[0] = RouteFlags.HIDDEN;

        final PolicyLintReport report = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());

        assertTrue(report.hasErrors());
        assertTrue(report.issues().stream().anyMatch(issue -> PolicyLintCode.UNSUPPORTED_CAPABILITY.equals(issue.code())));
    }

    @Test
    void warningsAreOptionallyAllowedOrRejected() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        bundle.candidate().venueWeightBps[0] = 0;

        final PolicyLintReport allowed = new DefaultPolicyLint(new PolicyLintConfig(true, 10_000, Integer.MAX_VALUE))
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());
        final PolicyLintReport rejected = new DefaultPolicyLint(new PolicyLintConfig(false, 10_000, Integer.MAX_VALUE))
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());

        assertTrue(allowed.hasWarnings());
        assertFalse(allowed.hasErrors());
        assertTrue(rejected.hasErrors());
    }

    @Test
    void boundsAndLintExceptionsRejectCandidate() {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        bundle.candidate().venueWeightBps[0] = 20_000;
        bundle.candidate().maxChildQty[0] = 0L;

        final PolicyLintReport bounds = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());
        final PolicyLintReport exception = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(null, bundle.input(), bundle.strategic());

        assertTrue(bounds.hasErrors());
        assertTrue(bounds.issues().stream().anyMatch(issue -> PolicyLintCode.WEIGHT_OUT_OF_BOUNDS.equals(issue.code())));
        assertTrue(exception.hasErrors());
        assertEquals(PolicyLintCode.LINT_EXCEPTION, exception.issues().get(0).code());
    }

    @Test
    void arrayLengthMismatchFails() throws Exception {
        final TestPolicyFixtures.CandidateBundle bundle = TestPolicyFixtures.candidateBundle();
        final Field field = MutablePolicyCandidate.class.getField("venueWeightBps");
        field.set(bundle.candidate(), new int[1]);

        final PolicyLintReport report = new DefaultPolicyLint(PolicyLintConfig.defaults())
                .lint(bundle.candidate(), bundle.input(), bundle.strategic());

        assertTrue(report.hasErrors());
        assertTrue(report.issues().stream().anyMatch(issue -> PolicyLintCode.ARRAY_LENGTH_MISMATCH.equals(issue.code())));
        assertThrows(IllegalArgumentException.class, () -> new PolicyLintIssue(PolicyLintSeverity.ERROR, "", "message"));
        assertThrows(IllegalArgumentException.class, () -> new PolicyLintConfig(true, 20_000, 0));
    }
}
