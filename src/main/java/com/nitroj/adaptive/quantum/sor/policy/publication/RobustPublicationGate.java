package com.nitroj.adaptive.quantum.sor.policy.publication;

import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.governance.FileScoreMatrixArtifactStore;
import com.nitroj.adaptive.quantum.sor.governance.ScoreMatrixArtifactStore;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.robust.ExpectedObjective;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.robust.PolicyCandidateSet;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustObjective;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustObjectiveType;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelection;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionConfig;
import com.nitroj.adaptive.quantum.sor.policy.robust.RobustSelectionProvenance;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSetAdequacy;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSetDescriptor;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScenarioSweepEvaluator;
import com.nitroj.adaptive.quantum.sor.policy.robust.ScoreMatrix;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;

/**
 * Responsibility: compose Phase 7 robust selection with the existing publisher.
 */
public final class RobustPublicationGate {
    private final ScenarioSweepEvaluator evaluator;
    private final ScenarioSetAdequacy adequacy;
    private final ScoreMatrixArtifactStore artifactStore;

    public RobustPublicationGate() {
        this(new ScenarioSweepEvaluator(), new ScenarioSetAdequacy(), null);
    }

    public RobustPublicationGate(final ScenarioSweepEvaluator evaluator, final ScenarioSetAdequacy adequacy) {
        this(evaluator, adequacy, null);
    }

    public RobustPublicationGate(
            final ScenarioSweepEvaluator evaluator,
            final ScenarioSetAdequacy adequacy,
            final ScoreMatrixArtifactStore artifactStore
    ) {
        if (evaluator == null || adequacy == null) {
            throw new IllegalArgumentException("evaluator and adequacy must not be null");
        }
        this.evaluator = evaluator;
        this.adequacy = adequacy;
        this.artifactStore = artifactStore;
    }

    public PublicationGateResult publish(
            final PolicyCandidateSet candidates,
            final ScenarioSetDescriptor scenarios,
            final RobustSelectionConfig config,
            final PolicyPublisher publisher,
            final PolicyValidationReport validationReport,
            final PolicyLintReport lintReport,
            final PolicyDiff diff,
            final int expectedImprovementBps,
            final long nowNanos
    ) {
        if (candidates == null || publisher == null) {
            throw new IllegalArgumentException("candidates and publisher must not be null");
        }
        final RobustSelectionConfig resolved = config == null ? RobustSelectionConfig.defaults() : config;
        if (!resolved.enabled()) {
            return publishSelected(candidates.candidates().get(0), publisher, validationReport, lintReport, diff,
                    expectedImprovementBps, nowNanos, null);
        }
        if (!candidates.robustSelectionReady()) {
            final RobustSelectionProvenance provenance = new RobustSelectionProvenance(
                    RobustObjectiveType.SINGLE_CANDIDATE_FALLBACK.name(),
                    "single",
                    "",
                    0L,
                    0,
                    1,
                    "",
                    "SINGLE_CANDIDATE",
                    java.util.List.of()
            );
            return publishSelected(candidates.singleCandidate(), publisher, validationReport, lintReport, diff,
                    expectedImprovementBps, nowNanos, provenance);
        }
        if (scenarios == null) {
            throw new IllegalArgumentException("scenarios must not be null when robust selection is enabled");
        }
        final ScenarioSetAdequacy.Result adequacyResult = adequacy.evaluate(scenarios, resolved.adequacy());
        if (!adequacyResult.adequate() && resolved.adequacy().strictBlock()) {
            final PublicationGateResult rejected = PublicationGateResult.rejected("robust_adequacy");
            rejected.adequacyStatus = adequacyResult.status();
            rejected.adequacyMissingCategories = adequacyResult.missingCategories().toArray(String[]::new);
            rejected.robustObjective = resolved.objective().name();
            return rejected;
        }
        final ScoreMatrix matrix = evaluator.evaluate(candidates, scenarios);
        final ScoreMatrixArtifactStore store = artifactStore == null
                ? new FileScoreMatrixArtifactStore(resolved.artifactDirectory())
                : artifactStore;
        final String matrixHandle = store.store(matrix);
        final RobustSelection selection = (!adequacyResult.adequate()
                ? new ExpectedObjective(RobustObjectiveType.ADEQUACY_FALLBACK, "mean")
                : RobustObjective.fromConfig(resolved)).select(matrix);
        final PolicyCandidate selected = candidateById(candidates, selection.selectedCandidateId());
        final RobustSelectionProvenance provenance = new RobustSelectionProvenance(
                selection.objective().name(),
                selection.objectiveParameters(),
                matrix.scenarioSetId(),
                matrix.scenarioSetVersion(),
                matrix.scenarioCount(),
                matrix.candidateCount(),
                matrixHandle,
                adequacyResult.status(),
                adequacyResult.missingCategories()
        );
        return publishSelected(selected, publisher, validationReport, lintReport, diff, expectedImprovementBps, nowNanos,
                provenance);
    }

    private static PublicationGateResult publishSelected(
            final PolicyCandidate selected,
            final PolicyPublisher publisher,
            final PolicyValidationReport validationReport,
            final PolicyLintReport lintReport,
            final PolicyDiff diff,
            final int expectedImprovementBps,
            final long nowNanos,
            final RobustSelectionProvenance provenance
    ) {
        final SorPolicy policy = selected.compiledPolicy();
        final PublicationGateResult result = publisher.publish(policy, validationReport, lintReport, diff, expectedImprovementBps, nowNanos);
        result.selectedCandidateId = selected.candidateId();
        if (provenance != null) {
            result.robustObjective = provenance.robustObjective();
            result.adequacyStatus = provenance.adequacyStatus();
            result.adequacyMissingCategories = provenance.adequacyMissingCategories().toArray(String[]::new);
            if (result.publishAllowed) {
                publisher.annotateLatestLedger(provenance);
            }
        }
        return result;
    }

    private static PolicyCandidate candidateById(final PolicyCandidateSet candidates, final int candidateId) {
        for (PolicyCandidate candidate : candidates.candidates()) {
            if (candidate.candidateId() == candidateId) {
                return candidate;
            }
        }
        throw new IllegalStateException("selected candidate not found: " + candidateId);
    }
}
