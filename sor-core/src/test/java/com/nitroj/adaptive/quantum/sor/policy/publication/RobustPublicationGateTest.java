package com.nitroj.adaptive.quantum.sor.policy.publication;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.governance.FileScoreMatrixArtifactStore;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.compile.CompiledScoreConfig;
import com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidateSet;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustObjectiveType;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionConfig;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionNarrative;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionProvenance;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSetAdequacy;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSetDescriptor;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSweepEvaluator;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScoreMatrix;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 7 robust publication gate composition.
 */
final class RobustPublicationGateTest {
    @TempDir
    Path tempDir;

    @Test
    void adequateSetUsesConfiguredObjectiveAndPublishesWinner() {
        final PolicyPublisher publisher = publisher();

        final PublicationGateResult result = gate().publish(
                candidateSet(),
                scenarios(List.of("regime", "liquidity")),
                config(true, RobustObjectiveType.CVAR_K, 1, List.of("regime")),
                publisher,
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                null,
                10,
                1L
        );

        assertTrue(result.publishAllowed);
        assertEquals("CVAR_K", result.robustObjective);
        assertEquals("ADEQUATE", result.adequacyStatus);
        assertTrue(result.selectedCandidateId >= 0);
        assertNotNull(publisher.activePolicy());
        assertFalse(publisher.ledger().get(0).scoreMatrixHandle.isBlank());
        assertTrue(Files.isRegularFile(Path.of(publisher.ledger().get(0).scoreMatrixHandle)));
    }

    @Test
    void inadequateSetFallsBackToExpectedWhenStrictBlockDisabled() {
        final PolicyPublisher publisher = publisher();

        final PublicationGateResult result = gate().publish(
                candidateSet(),
                scenarios(List.of("regime")),
                config(false, RobustObjectiveType.CVAR_K, 3, List.of("regime", "liquidity")),
                publisher,
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                null,
                10,
                1L
        );

        assertTrue(result.publishAllowed);
        assertEquals("ADEQUACY_FALLBACK", result.robustObjective);
        assertEquals("INADEQUATE", result.adequacyStatus);
        assertArrayEquals(new String[]{"liquidity"}, result.adequacyMissingCategories);
        assertNotNull(publisher.activePolicy());
    }

    @Test
    void inadequateSetBlocksPublicationWhenStrictBlockEnabled() {
        final PolicyPublisher publisher = publisher();

        final PublicationGateResult result = gate().publish(
                candidateSet(),
                scenarios(List.of("regime")),
                config(true, RobustObjectiveType.CVAR_K, 2, List.of("regime", "liquidity")),
                publisher,
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                null,
                10,
                1L
        );

        assertFalse(result.publishAllowed);
        assertArrayEquals(new String[]{"robust_adequacy"}, result.failedGates);
        assertEquals("INADEQUATE", result.adequacyStatus);
        assertArrayEquals(new String[]{"liquidity"}, result.adequacyMissingCategories);
        assertNull(publisher.activePolicy());
    }

    @Test
    void disabledModePreservesExistingSingleCandidatePublicationBehavior() {
        final PolicyPublisher publisher = publisher();
        final PolicyCandidateSet single = new PolicyCandidateSet(List.of(
                new PolicyCandidate(0, "single", null, TestPolicyFixtures.policy())
        ));

        final PublicationGateResult result = new RobustPublicationGate().publish(
                single,
                null,
                RobustSelectionConfig.defaults(),
                publisher,
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                null,
                10,
                1L
        );

        assertTrue(result.publishAllowed);
        assertEquals("", result.robustObjective);
        assertEquals(0, result.selectedCandidateId);
        assertNotNull(publisher.activePolicy());
        assertNull(publisher.ledger().get(0).robustObjective);
    }

    @Test
    void robustPublicationPersistsMatrixAndAnnotatesLedgerProvenance() {
        final PolicyPublisher publisher = publisher();

        final PublicationGateResult result = new RobustPublicationGate(
                fakeEvaluator(),
                new ScenarioSetAdequacy(),
                new FileScoreMatrixArtifactStore(tempDir)
        ).publish(
                candidateSet(),
                scenarios(List.of("regime", "liquidity")),
                config(true, RobustObjectiveType.CVAR_K, 1, List.of("regime")),
                publisher,
                PolicyValidationReport.validReport(),
                new PolicyLintReport(List.of()),
                null,
                10,
                1L
        );

        final var ledger = publisher.ledger().get(0);
        assertTrue(result.publishAllowed);
        assertEquals("CVAR_K", ledger.robustObjective);
        assertEquals("k=10", ledger.robustObjectiveParameters);
        assertEquals("unit-robust", ledger.scenarioSetId);
        assertEquals(2, ledger.scenarioCount);
        assertEquals(2, ledger.candidateCount);
        assertEquals("ADEQUATE", ledger.adequacyStatus);
        assertTrue(Files.isRegularFile(Path.of(ledger.scoreMatrixHandle)));

        final String narrative = RobustSelectionNarrative.render(new RobustSelectionProvenance(
                ledger.robustObjective,
                ledger.robustObjectiveParameters,
                ledger.scenarioSetId,
                ledger.scenarioSetVersion,
                ledger.scenarioCount,
                ledger.candidateCount,
                ledger.scoreMatrixHandle,
                ledger.adequacyStatus,
                List.of()
        ), result.selectedCandidateId);
        assertTrue(narrative.contains("Selected candidate"));
        assertTrue(narrative.contains("CVAR_K"));
        assertTrue(narrative.contains("Score matrix"));
    }

    private static RobustPublicationGate gate() {
        return new RobustPublicationGate(fakeEvaluator(), new ScenarioSetAdequacy());
    }

    private static ScenarioSweepEvaluator fakeEvaluator() {
        return (candidates, scenarios) -> {
            final long[][] scores = new long[candidates.size()][scenarios.size()];
            for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
                for (int scenarioIndex = 0; scenarioIndex < scenarios.size(); scenarioIndex++) {
                    scores[candidateIndex][scenarioIndex] = 1_000L + candidateIndex * 100L - scenarioIndex;
                }
            }
            return ScoreMatrix.of(
                    scenarios.scenarioSetId(),
                    scenarios.scenarioSetVersion(),
                    "ScenarioScorecardV1",
                    candidates.candidates(),
                    scenarios,
                    scores
            );
        };
    }

    private static PolicyPublisher publisher() {
        return new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
    }

    private static RobustSelectionConfig config(
            final boolean strictBlock,
            final RobustObjectiveType objective,
            final int minScenarioCount,
            final List<String> requiredCategories
    ) {
        return new RobustSelectionConfig(
                true,
                objective,
                10,
                false,
                "unit-robust",
                1L,
                List.of("robust"),
                RobustSelectionConfig.CandidateGrid.defaults(),
                new RobustSelectionConfig.Adequacy(minScenarioCount, requiredCategories, strictBlock),
                null
        );
    }

    private static PolicyCandidateSet candidateSet() {
        final var first = TestPolicyFixtures.policy();
        final var bundle = TestPolicyFixtures.candidateBundle();
        bundle.candidate().venueWeightBps[0] += 123;
        final var second = new DefaultPolicyCompiler(new CompiledScoreConfig(2, 1, 1))
                .compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());
        return new PolicyCandidateSet(List.of(
                new PolicyCandidate(0, "base", null, first),
                new PolicyCandidate(1, "tilted", null, second)
        ));
    }

    private static ScenarioSetDescriptor scenarios(final List<String> categories) {
        final List<String> scenarioIds = categories.stream()
                .map(category -> "scenario-" + category)
                .toList();
        return new ScenarioSetDescriptor("unit-robust", 1L, scenarioIds, categories);
    }
}
