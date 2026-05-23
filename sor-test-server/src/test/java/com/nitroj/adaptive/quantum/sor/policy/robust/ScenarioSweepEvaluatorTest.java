package com.nitroj.adaptive.quantum.sor.policy.robust;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.governance.FileScoreMatrixArtifactStore;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSimulatorConfig;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSpec;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioSummary;
import com.nitroj.adaptive.quantum.sor.scenario.ScenarioWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify Phase 7 scenario sweep and score-matrix artifacts.
 */
final class ScenarioSweepEvaluatorTest {
    @TempDir
    Path tempDir;

    @Test
    void scorecardComputesDocumentedFormula() {
        final ScenarioSummary summary = new ScenarioSummary(
                "formula",
                1L,
                1,
                0L,
                0,
                0,
                0,
                0,
                2,
                3,
                4,
                11L,
                0L,
                0L,
                0L,
                0L,
                5,
                6,
                0,
                0,
                0
        );

        assertEquals(4 * 10_000L + 3 * 2_500L - 2 * 5_000L - 11L - 5 * 250L - 6 * 1_000L,
                new ScenarioScorecardV1().score(summary));
    }

    @Test
    void sweepCoversEveryCandidateScenarioPairAndIsDeterministic() {
        final PolicyCandidateSet candidates = candidateSet();
        final ScenarioSetDescriptor scenarios = scenarios();
        final ScenarioSweepEvaluator evaluator = new ScenarioRunnerSweepEvaluator(specs());

        final ScoreMatrix first = evaluator.evaluate(candidates, scenarios);
        final ScoreMatrix second = evaluator.evaluate(candidates, scenarios);

        assertEquals(candidates.size(), first.candidateCount());
        assertEquals(scenarios.size(), first.scenarioCount());
        assertArrayEquals(first.candidateIds(), second.candidateIds());
        assertArrayEquals(first.scenarioIds(), second.scenarioIds());
        assertArrayEquals(first.scores(), second.scores());
        assertEquals(first.matrixId(), second.matrixId());
        assertNotEquals(first.score(0, 0), first.score(1, 0),
                "candidate policy must influence score matrix rows");
    }

    @Test
    void fileArtifactStoreWritesRequiredCsvRows() throws Exception {
        final ScoreMatrix matrix = new ScenarioRunnerSweepEvaluator(specs()).evaluate(candidateSet(), scenarios());
        final String handle = new FileScoreMatrixArtifactStore(tempDir).store(matrix);
        final String csv = Files.readString(Path.of(handle));

        assertTrue(csv.startsWith("matrixId,scenarioSetId,scenarioSetVersion,scorecardVersion,candidateId,"));
        assertTrue(csv.contains("ScenarioScorecardV1"));
        assertEquals(1 + matrix.candidateCount() * matrix.scenarioCount(), csv.lines().count());
    }

    @Test
    void matrixIdChangesWhenCandidatePolicyHashesOrScoresChange() {
        final ScenarioSetDescriptor scenarios = scenarios();
        final ScenarioSweepEvaluator evaluator = new ScenarioRunnerSweepEvaluator(specs());

        final ScoreMatrix first = evaluator.evaluate(candidateSet(123), scenarios);
        final ScoreMatrix second = evaluator.evaluate(candidateSet(321), scenarios);

        assertNotEquals(first.matrixId(), second.matrixId());
        assertNotEquals(first.candidatePolicyHash64()[1], second.candidatePolicyHash64()[1]);
    }

    private static PolicyCandidateSet candidateSet() {
        return candidateSet(123);
    }

    private static PolicyCandidateSet candidateSet(final int venueWeightTiltBps) {
        final var first = TestPolicyFixtures.policy();
        final var bundle = TestPolicyFixtures.candidateBundle();
        bundle.candidate().venueWeightBps[0] += venueWeightTiltBps;
        final var second = new com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompiler(
                new com.nitroj.adaptive.quantum.sor.policy.compile.CompiledScoreConfig(2, 1, 1))
                .compile(bundle.candidate(), bundle.strategic(), bundle.tactical(), bundle.lint());
        return new PolicyCandidateSet(List.of(
                new PolicyCandidate(0, "base", null, first),
                new PolicyCandidate(1, "tilted", null, second)
        ));
    }

    private static ScenarioSetDescriptor scenarios() {
        return new ScenarioSetDescriptor(
                "unit-robust",
                1L,
                specs().stream().map(ScenarioSpec::scenarioId).toList(),
                List.of("regime", "liquidity")
        );
    }

    private static List<ScenarioSpec> specs() {
        final SorConfig config = new SorConfig(1, 3, 3, 2, SorConfig.RuntimeMode.DEMO, true);
        return List.of(
                spec("robust-normal", 1L, config, 0),
                spec("robust-volatile", 2L, config, 1)
        );
    }

    private static ScenarioSpec spec(final String id, final long seed, final SorConfig config, final int regimeId) {
        return new ScenarioSpec(
                id,
                seed,
                2,
                config,
                new ScenarioWindow[]{new ScenarioWindow(0, 1, regimeId)},
                true,
                ScenarioSimulatorConfig.defaults()
        );
    }
}
