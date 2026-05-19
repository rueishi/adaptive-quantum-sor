# Phase 7 Completion Report

Phase 7 adds robust policy selection as an additive L1 publication-gate path.
It evaluates a deterministic candidate set against a declared scenario set,
selects one candidate by a configured objective, persists the score matrix, and
stamps the governance ledger with the decision provenance.

The repository default is `robustSelection.enabled=false`, so existing Phase
1-6 optimizer, publication, scenario, native, notebook, and L0 execution
behavior remains the default path.

## Implemented Scope

- `PolicyCandidate` and `PolicyCandidateSet` define the optimizer-to-L1
  candidate-set contract with deterministic candidate IDs and duplicate-hash
  rejection.
- `PolicyOptimizerCoordinator.buildCandidateSet(...)` builds a deterministic
  candidate set without publishing or changing the existing `runCycle(...)`
  path.
- `ScenarioSweepEvaluator`, `ScenarioScorecardV1`, and `ScoreMatrix` produce
  deterministic candidate x scenario evidence using the existing scenario
  runner, documented scenario score formula, and compiled-policy quality
  adjustment.
- `FileScoreMatrixArtifactStore` persists score matrices as CSV artifacts; the
  default robust gate uses the configured artifact directory when no explicit
  store is supplied.
- `ExpectedObjective`, `MinMaxObjective`, `CvarKObjective`, and
  `MinRegretObjective` are pure objective functions over `ScoreMatrix`.
- `ScenarioSetAdequacy` checks minimum scenario count and required categories.
- `RobustPublicationGate` composes adequacy, objective selection, artifact
  persistence, and the existing `PolicyPublisher.publish(...)` path.
- `PolicyChangeLedgerEntry` can carry robust-selection provenance while legacy
  ledger entries remain valid when robust selection is disabled.
- `RobustSelectionNarrative` renders a human-readable explanation of the
  selected candidate, objective, scenario set, adequacy result, and matrix
  handle.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P7-ROBUST-001 candidate-set contract with distinct canonical hashes
P7-ROBUST-002 single-candidate fallback provenance
P7-ROBUST-003 score matrix covers every candidate x scenario pair
P7-ROBUST-004 deterministic sweep replay for fixed inputs
P7-ROBUST-005 pure objective functions with deterministic tie-breaking
P7-ROBUST-006 objective config defaults and MIN_MAX opt-in
P7-ROBUST-007 L0 hot path unchanged
P7-ROBUST-008 adequacy fallback records missing categories
P7-ROBUST-009 strict adequacy blocks publication
P7-ROBUST-010 score matrix artifact persistence
P7-ROBUST-011 governance record carries robust provenance
P7-ROBUST-012 disabled mode preserves Phase 1-6 behavior
X-DET-001 deterministic selection and replay
X-AUDIT-001 robust artifacts and provenance are auditable
X-OBS-001 robust selection narrative explains the decision
X-DOC-001 documentation and completion evidence are present
```

Planned ACs:

```text
none
```

Failed ACs:

```text
none
```

## Reproduction Commands

```bash
./gradlew test --tests com.nitroj.adaptive.quantum.sor.policy.robust.*
./gradlew test --tests com.nitroj.adaptive.quantum.sor.policy.publication.RobustPublicationGateTest
./gradlew test --tests com.nitroj.adaptive.quantum.sor.docs.Phase7CompletionReportTest
scripts/run_tests.sh all
```

Focused regression groups:

```text
com.nitroj.adaptive.quantum.sor.execution.*
com.nitroj.adaptive.quantum.sor.policy.publication.*
com.nitroj.adaptive.quantum.sor.optimizer.PolicyOptimizerCoordinatorTest
com.nitroj.adaptive.quantum.sor.scenario.*
com.nitroj.adaptive.quantum.sor.optimizer.batch.*
com.nitroj.adaptive.quantum.sor.optimizer.ising.*
```

## Known Limits

- The first scorecard is deterministic and auditable, but intentionally simple;
  it is not a production transaction-cost-analysis model.
- Robustness is relative to the declared scenario set. Phase 7 records scenario
  provenance and adequacy, but does not prove the catalog is representative.
- Candidate diversity is grid-based. More advanced diversity generation remains
  future work.
- Robust selection is implemented as an additive path and disabled by default
  until a deployment or demo profile opts in.
