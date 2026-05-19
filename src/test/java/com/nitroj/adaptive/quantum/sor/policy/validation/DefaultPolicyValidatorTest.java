package com.nitroj.adaptive.quantum.sor.policy.validation;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify compiled policy validation checks.
 *
 * <p>Role in system: covers P1-TC-013 validation before publication gates can
 * accept or reject a policy.</p>
 *
 * <p>Relationships: validates policies produced by the Phase 1 compiler
 * fixtures and mutates route arrays to exercise invalid artifact paths.</p>
 *
 * <p>Lifecycle: executed by Gradle as direct unit coverage for policy validation.</p>
 *
 * <p>Design intent: validator tests duplicate critical publication-boundary
 * checks even when constructors already enforce some invariants.</p>
 */
final class DefaultPolicyValidatorTest {
    @Test
    void validPolicyPasses() {
        final PolicyValidationReport report = new DefaultPolicyValidator(TestPolicyFixtures.VENUES)
                .validate(TestPolicyFixtures.policy());

        assertTrue(report.valid());
        assertTrue(report.errors().isEmpty());
    }

    @Test
    void badOffsetsFail() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        policy.hotRouteBook.routeListOffset[1] = -1;

        final PolicyValidationReport report = new DefaultPolicyValidator(TestPolicyFixtures.VENUES).validate(policy);

        assertFalse(report.valid());
        assertTrue(report.errors().contains("routeListOffset must be monotonic"));
    }

    @Test
    void invalidVenueIdsFail() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        policy.hotRouteBook.routeVenueId[0] = 99;

        final PolicyValidationReport report = new DefaultPolicyValidator(TestPolicyFixtures.VENUES).validate(policy);

        assertFalse(report.valid());
        assertTrue(report.errors().contains("invalid venueId in route list"));
    }

    @Test
    void emptyRouteListAndMissingHashFail() {
        final SorPolicy policy = TestPolicyFixtures.policy();
        policy.hotRouteBook.routeListOffset[1] = policy.hotRouteBook.routeListOffset[0];
        final SorPolicy missingHash = new SorPolicy(
                policy.policyVersion,
                policy.createdAtEpochNanos,
                policy.effectiveFromEpochNanos,
                0L,
                new byte[32],
                policy.isingResultVersion,
                policy.cudaTuningVersion,
                policy.optimizerType,
                policy.policyState,
                policy.hotRouteBook,
                policy.fullPolicyMatrix
        );

        final PolicyValidationReport report = new DefaultPolicyValidator(TestPolicyFixtures.VENUES).validate(missingHash);

        assertFalse(report.valid());
        assertTrue(report.errors().contains("route list must be non-empty"));
        assertTrue(report.errors().contains("policy hash must be present"));
    }

    @Test
    void reportAndConstructorContractsAreCovered() {
        final PolicyValidationReport valid = PolicyValidationReport.validReport();
        final PolicyValidationReport invalid = new PolicyValidationReport(List.of("bad"));

        assertTrue(valid.valid());
        assertFalse(invalid.valid());
        assertThrows(IllegalArgumentException.class, () -> new DefaultPolicyValidator(0));
        assertThrows(IllegalArgumentException.class, () -> new PolicyValidationReport(null));
    }
}
