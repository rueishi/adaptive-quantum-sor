# Adaptive Quantum SOR — Phase 7 Specification: Robust Policy Selection

> **Integration note for the maintainer.** This document is ready to append to
> `adaptive_quantum_sor_spec_v1.md` as Phase 7. Numbering, task-card prefixes,
> acceptance-criteria IDs, package names, file targets, configuration fields,
> persistence format, and test expectations have been normalized for this repo.

---

## 7.0 Document Control

| Field | Value |
|---|---|
| Phase | 7 |
| Phase title | Robust Policy Selection |
| Optimization roadmap item | #4 |
| Status | IMPLEMENTED |
| Prerequisite roadmap items | #3 Pairwise anti-gaming QUBO term — **COMPLETE**; #1 Cross-parent batch venue allocation — **COMPLETE** |
| Successor roadmap items | #2 Portfolio scheduling under aggregate risk — PLANNED; #5 Venue fee-tier commitment — PLANNED |
| Owning layer | L1 policy compiler/publisher, specifically the publication gate extension |
| Hot-path impact | **None.** Phase 7 is forbidden from introducing any L0 dependency. |
| Backward compatibility | Existing Phase 1-6 behavior must remain unchanged when `robustSelection.enabled=false`. |

---

## 7.1 Objective

Today the warm-path optimizer publishes one compiled policy candidate after
validity, lint, expected-improvement, cadence, and churn checks. Phase 7 changes
publication selection from "publish the one candidate" to "choose one candidate
from a deterministic candidate set by an explicit robustness objective over a
declared scenario set."

Given:

```text
PolicyCandidateSet candidates
ScenarioSetDescriptor scenario set
RobustSelectionConfig objective config
```

Phase 7 evaluates every candidate against every selected scenario, persists the
candidate x scenario score matrix, selects exactly one candidate by the resolved
objective, stamps the full decision provenance, and then hands the winner to the
existing compile/diff/ledger/atomic-swap path.

This is a warm-path, publication-time concern. It changes *which* immutable
policy becomes active; it does not change how the active policy is executed.

### 7.1.1 Why This Phase Exists

The optimizer pipeline can produce policies that look excellent under expected
conditions and poor under regime shifts, liquidity fades, stale feeds, or venue
outages. Selecting only by expected-condition improvement silently accepts tail
fragility. Phase 7 makes the robustness/expected-quality trade an explicit,
configured, auditable decision.

### 7.1.2 What Is And Is Not Provable

The selector mechanics are exactly testable. The conceptual risk is that "the
robust policy" has no universal ground truth. Robustness is defined only by:

```text
the candidate set
the scenario set
the scorecard formula
the selected objective
the objective parameters
```

Therefore Phase 7 treats transparency, configurability, and scenario-set
provenance as contractual requirements, not optional reporting polish.

---

## 7.2 Scope

### 7.2.1 In Scope

- A deterministic candidate-set contract between optimizer coordination and L1.
- Configurable robust objectives applied at the publication gate.
- Deterministic scenario-sweep evaluation reusing the existing scenario catalog,
  `ScenarioDefinitionLoader`, `ScenarioRunner`, and `ScenarioSummary`.
- A concrete Phase 7 scorecard formula over `ScenarioSummary`.
- A persisted candidate x scenario score matrix artifact.
- Decision provenance in governance/ledger records.
- A scenario-set adequacy gate that bounds the strength of the robustness claim.
- Documentation, scenario, and exact objective tests.
- Backward-compatible disabled mode that preserves the Phase 1-6 optimizer,
  publication, scenario, and execution behavior.

### 7.2.2 Out Of Scope

- **No L0 hot-path presence.** No scenario evaluation, robustness computation,
  candidate-set handling, or score-matrix access occurs in `route()` or any
  per-order path. By the time L0 runs, there is exactly one immutable published
  policy.
- **No new runtime layer.** Robust selection is a parameterization of the
  existing L1 publication gate, not a new layer between L4/L3 and L1.
- **No adaptive/generated scenarios.** Phase 7 evaluates over the existing
  declared scenario catalog only.
- **No Pareto or multi-objective publication decision.** Exactly one objective
  is active per publication decision.
- **No claim that scenario coverage is complete.** Adequacy gates make the
  strength of the claim visible; they do not prove representativeness.
- **No breaking API change to existing publication callers.** Existing
  `PolicyPublisher.publish(...)`, `PublicationGate.evaluate(...)`, and
  `PolicyOptimizerCoordinator.runCycle(...)` behavior must remain available.
  Robust selection is an additive path.

---

## 7.3 Layer Placement And Data Flow

```text
L4 strategic optimizer ─┐
                        ├─► PolicyCandidateSet ─► L1 RobustPublicationGate
L3 tactical optimizer ──┘                                  │
                                                           │ read-only evaluator
L6 scenario catalog + ScenarioRunner + scorecard ◄─────────┘
                                                           │
                                                           ▼
                                      one selected candidate ─► existing publish path ─► L0 unchanged
```

Phase 7 changes exactly two boundaries:

1. **Optimizer coordinator to L1:** `PolicyOptimizerCoordinator` must be able to
   produce or accept a `PolicyCandidateSet` instead of only one candidate
   (`P7-TC-001`).
2. **Publication gate:** `RobustPublicationGate` evaluates a candidate set,
   adequacy, objective math, matrix persistence, and provenance, then delegates
   one winning `SorPolicy` to the existing `PolicyPublisher.publish(...)`
   behavior (`P7-TC-003` through `P7-TC-005`).

L6 is used as a deterministic evaluation tool. Phase 7 does not "live" in L6.
L0 remains untouched.

When `robustSelection.enabled=false`, the data flow is exactly the pre-Phase-7
flow:

```text
one candidate -> existing lint/compile/validate/publish path -> L0 unchanged
```

This disabled mode is required for Phase 1-6 regression safety and for staged
rollout.

---

## 7.4 Candidate-Set Contract

The robust selector is meaningless with one candidate. The substantive
integration cost of Phase 7 is making candidate diversity explicit.

### 7.4.1 Contract

`PolicyCandidateSet` contains 2..N `PolicyCandidate` entries for the robust path.
Each entry has:

```text
candidateId                 deterministic ordinal, starting at 0
generationLabel             human-readable source, e.g. riskScale=75
mutableCandidate            MutablePolicyCandidate before compile
compiledPolicy              SorPolicy after compile, when available
canonicalPolicyHash64       from compiled SorPolicy.policyHash64
canonicalPolicyHashSha256   from compiled SorPolicy.policyHashSha256
```

All candidates must be individually valid under existing `PolicyLint` and
`PolicyValidator` rules before robust selection asserts a robustness objective.
Candidates must be pairwise distinct by canonical policy hash. Ties in robust
objectives are broken by the lowest `candidateId`.

### 7.4.2 Candidate Generation

The initial repo implementation should use a deterministic configured grid over
existing candidate-generation knobs rather than inventing a new optimizer:

```text
robust.candidateGrid.riskScaleBps: [7500, 10000, 12500]
robust.candidateGrid.concentrationPenaltyScaleBps: [7500, 10000, 12500]
robust.candidateGrid.maxCandidateCount: 9
```

The grid is resolved in declaration order. Candidate IDs follow generation
order after invalid/duplicate candidates are removed.

### 7.4.3 Single-Candidate Degradation

If only one candidate remains after lint/validation/hash de-duplication, the
system may publish it through the existing gate, but provenance must use:

```text
robustObjective=SINGLE_CANDIDATE_FALLBACK
```

It must not assert `CVAR_K`, `MIN_MAX`, `MIN_REGRET`, or `EXPECTED` robustness.

---

## 7.5 Robust Objectives

For candidate `c` and scenario `s`, let `score(c,s)` be the Phase 7 scorecard
value from `ScoreMatrix`. Higher is better.

| Objective | Selection Value | Notes |
|---|---|---|
| `EXPECTED` | mean score across scenarios | Baseline and adequacy fallback. |
| `MIN_MAX` | minimum score across scenarios | Strict worst-case; explicit opt-in only. |
| `CVAR_K` | mean of the worst k percent of scenario scores | Default; `k=10`. At least one scenario is included. |
| `MIN_REGRET` | negative maximum regret, where regret is `bestScoreInScenario - candidateScore` | Selects the candidate least far from per-scenario best. |

Requirements:

```text
default objective: CVAR_K
default cvarKPercent: 10
MIN_MAX requires robust.allowMinMaxObjective=true
all objective functions are pure functions of ScoreMatrix
all ties break by lowest candidateId
```

`MIN_REGRET` is implemented as an argmax over `-maxRegret` so every objective
can share the same "largest objective value wins" selector contract.

---

## 7.6 Score Matrix Artifact

Every robust publication attempt that evaluates more than one candidate must
persist a complete score matrix before publication.

### 7.6.1 Matrix Fields

`ScoreMatrix` contains:

```text
matrixId
scenarioSetId
scenarioSetVersion
scorecardVersion
candidateIds[]
candidatePolicyHash64[]
candidatePolicyHashSha256Hex[]
scenarioIds[]
scenarioCategories[]
scores[candidateIndex][scenarioIndex]
```

### 7.6.2 Scorecard Version 1

Phase 7 defines `ScenarioScorecardV1` as a deterministic scenario base score
plus a deterministic candidate-policy adjustment. The base score uses existing
`ScenarioSummary` fields:

```text
score =
  fullFillCount * 10000
  + partialFillCount * 2500
  - rejectCount * 5000
  - residualQty
  - staleEventCount * 250
  - outageEventCount * 1000
```

For each `PolicyCandidate`, the evaluator adds a compiled-policy quality
adjustment from `SorPolicy.fullPolicyMatrix`. The adjustment is a weighted mean
over eligible IVRU cells, using detected scenario regime counts to weight
regimes and urgency index to weight urgency. Each cell contributes:

```text
cellQuality =
  fillProbabilityBps
  + queueSurvivalBps / 2
  + venueWeightBps / 4
  + maxParticipationBps / 20
  - toxicityPenaltyBps
  - rejectPenaltyBps
  - slippagePenaltyBps / 2
  - marketImpactPenaltyBps / 2
  - latencyPenaltyNanos / 1000
```

This keeps the first implementation deterministic and replayable while ensuring
the score matrix is genuinely candidate x scenario, not a scenario-only score
copied across candidate rows.

The scorecard is deliberately simple, deterministic, and documented. Later
phases may replace it, but the scorecard version must be part of every matrix.

### 7.6.3 Persistence Format

The first implementation should add:

```text
src/main/java/com/nitroj/adaptive/quantum/sor/governance/ScoreMatrixArtifactStore.java
src/main/java/com/nitroj/adaptive/quantum/sor/governance/FileScoreMatrixArtifactStore.java
```

Default artifact path:

```text
build/robust-selection/score-matrix/<matrixId>.csv
```

`matrixId` must include scenario-set identity, candidate IDs, candidate policy
hash evidence, scenario IDs, and score content so distinct robust runs do not
overwrite prior governance evidence.

CSV columns:

```text
matrixId,scenarioSetId,scenarioSetVersion,scorecardVersion,candidateId,
candidatePolicyHash64,candidatePolicyHashSha256Hex,scenarioId,scenarioCategory,score
```

Tests may use a temporary directory. Production-like persistence remains
replaceable behind `ScoreMatrixArtifactStore`.

---

## 7.7 Decision Provenance

Every Phase 7 publication path must stamp provenance into governance records and
operator-visible narratives.

Add fields to `PolicyChangeLedgerEntry`:

```text
robustObjective
robustObjectiveParameters
scenarioSetId
scenarioSetVersion
scenarioCount
candidateCount
scoreMatrixHandle
adequacyStatus
adequacyMissingCategories
```

Allowed `robustObjective` values:

```text
EXPECTED
MIN_MAX
CVAR_K
MIN_REGRET
SINGLE_CANDIDATE_FALLBACK
ADEQUACY_FALLBACK
```

A robustness claim with no objective, scenario-set provenance, and matrix handle
is a defect.

---

## 7.8 Scenario-Set Adequacy Gate

The strength of a robustness result is bounded by the scenario set. Phase 7 must
not imply more confidence than the scenario catalog supports.

### 7.8.1 Default Adequacy Config

```text
robust.adequacy.minScenarioCount: 12
robust.adequacy.requiredCategories:
  - regime
  - liquidity
  - venue-health
  - failure-negative
robust.adequacy.strictBlock: false
```

### 7.8.2 Behavior

If adequacy passes:

```text
configured objective is used
adequacyStatus=ADEQUATE
```

If adequacy fails and `strictBlock=false`:

```text
selection objective becomes EXPECTED
provenance robustObjective=ADEQUACY_FALLBACK
missing categories are recorded
warning lifecycle/audit event is emitted
publication may continue through the existing gate
```

If adequacy fails and `strictBlock=true`:

```text
publication is blocked
PublicationGateResult includes failed gate robust_adequacy
no active policy swap occurs
```

---

## 7.9 Repo Integration Map

### 7.9.1 New Packages And Files

```text
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/PolicyCandidate.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/PolicyCandidateSet.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/RobustSelectionConfig.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/RobustObjectiveType.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/RobustObjective.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ExpectedObjective.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/MinMaxObjective.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/CvarKObjective.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/MinRegretObjective.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/RobustSelection.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ScoreMatrix.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ScenarioSetDescriptor.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ScenarioSetAdequacy.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ScenarioScorecardV1.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/robust/ScenarioSweepEvaluator.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/publication/RobustPublicationGate.java
src/main/java/com/nitroj/adaptive/quantum/sor/governance/ScoreMatrixArtifactStore.java
src/main/java/com/nitroj/adaptive/quantum/sor/governance/FileScoreMatrixArtifactStore.java
```

### 7.9.2 Existing Files To Update

```text
src/main/java/com/nitroj/adaptive/quantum/sor/optimizer/PolicyOptimizerCoordinator.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/PolicyPublisher.java
src/main/java/com/nitroj/adaptive/quantum/sor/policy/publication/PublicationGateResult.java
src/main/java/com/nitroj/adaptive/quantum/sor/governance/PolicyChangeLedgerEntry.java
src/main/java/com/nitroj/adaptive/quantum/sor/config/SorConfig.java
src/main/java/com/nitroj/adaptive/quantum/sor/config/ConfigLoader.java
src/main/resources/adaptive-quantum-sor.yaml
adaptive_quantum_sor_spec_v1.md
README.md
docs/ARCHITECTURE.md
docs/PHASE_7_COMPLETION_REPORT.md
```

---

## 7.10 Configuration Contract

Add a nested robust-selection config to `SorConfig` and YAML loading:

```yaml
robustSelection:
  enabled: false
  objective: CVAR_K
  cvarKPercent: 10
  allowMinMaxObjective: false
  scenarioSetId: default-robust-v1
  scenarioSetVersion: 1
  scenarioTags:
    - robust
  candidateGrid:
    riskScaleBps: [7500, 10000, 12500]
    concentrationPenaltyScaleBps: [7500, 10000, 12500]
    maxCandidateCount: 9
  adequacy:
    minScenarioCount: 12
    requiredCategories:
      - regime
      - liquidity
      - venue-health
      - failure-negative
    strictBlock: false
  artifactDirectory: build/robust-selection/score-matrix
```

`enabled=false` is the repository default for backward compatibility. Tests and
demo profiles may opt in explicitly with `enabled=true`.

Startup validation must reject:

```text
cvarKPercent outside 1..100
MIN_MAX when allowMinMaxObjective=false
negative or zero maxCandidateCount
blank scenarioSetId
blank required category names
```

When `enabled=false`, startup still validates basic type/shape if the
`robustSelection` section is present, but it must not require scenario tags,
adequacy coverage, artifact directory writability, or candidate-grid diversity.

---

## 7.11 Task Cards

#### P7-TC-001 — Candidate-Set Optimizer Contract

**Layer:** L4/L3 to L1 boundary

**Goal:** Change optimizer coordination so the robust path can produce a
deterministic `PolicyCandidateSet` of valid, distinct candidates.

**Deliverables:**

```text
PolicyCandidate
PolicyCandidateSet
RobustSelectionConfig candidate grid
PolicyOptimizerCoordinator candidate-set path
single-candidate fallback metadata
```

**Tests:**

```text
PolicyCandidateSet rejects duplicate canonical policy hashes
candidate IDs are deterministic ordinals after invalid/duplicate filtering
coordinator produces at least two candidates from the default grid when possible
single candidate path stamps SINGLE_CANDIDATE_FALLBACK
```

**Acceptance:** `P7-ROBUST-001`, `P7-ROBUST-002`.

---

#### P7-TC-002 — Scenario Sweep Evaluator And Score Matrix

**Layer:** L1 uses L6 evaluator

**Depends on:** `P7-TC-001`

**Goal:** Implement deterministic sweep evaluation and matrix persistence-ready
data structures.

**Deliverables:**

```text
ScenarioSetDescriptor
ScenarioScorecardV1
ScenarioSweepEvaluator
ScoreMatrix
ScoreMatrixArtifactStore
FileScoreMatrixArtifactStore
```

**Tests:**

```text
scorecard computes documented formula from ScenarioSummary
sweep covers every candidate and every scenario
same candidates/scenarios/seed produce bit-identical ScoreMatrix
CSV artifact includes required columns and one row per matrix cell
```

**Acceptance:** `P7-ROBUST-003`, `P7-ROBUST-004`, `X-DET-001`.

---

#### P7-TC-003 — Robust Objectives And Selector

**Layer:** L1

**Depends on:** `P7-TC-002`

**Goal:** Implement exact objective functions and selection result records.

**Deliverables:**

```text
RobustObjectiveType
RobustObjective
ExpectedObjective
MinMaxObjective
CvarKObjective
MinRegretObjective
RobustSelection
objective resolution from RobustSelectionConfig
```

**Tests:**

```text
EXPECTED selects highest mean score
MIN_MAX selects highest worst-case score
CVAR_K selects highest worst-k-percent mean and includes at least one scenario
MIN_REGRET selects smallest maximum regret
all objective ties choose lowest candidateId
MIN_MAX is rejected unless allowMinMaxObjective=true
CVAR_K k=10 is the default
```

**Acceptance:** `P7-ROBUST-005`, `P7-ROBUST-006`.

---

#### P7-TC-004 — Robust Publication Gate And Adequacy

**Layer:** L1 publication

**Depends on:** `P7-TC-003`

**Goal:** Compose candidate-set evaluation, adequacy, objective selection, and
existing publication safety without adding scoring logic to `PolicyPublisher`.

**Deliverables:**

```text
ScenarioSetAdequacy
RobustPublicationGate
PublicationGateResult adequacy fields
adequacy fallback path
strict-block path
hot-path non-regression test
```

**Tests:**

```text
adequate set uses configured objective
inadequate set with strictBlock=false selects EXPECTED and stamps ADEQUACY_FALLBACK
inadequate set with strictBlock=true blocks publication with robust_adequacy gate
PublicationGateResult exposes adequacy status and missing categories
robustSelection.enabled=false preserves existing PolicyPublisher.publish behavior
PolicyDrivenSorExecutioner tests remain unchanged and pass
```

**Acceptance:** `P7-ROBUST-007`, `P7-ROBUST-008`, `P7-ROBUST-009`, `P7-ROBUST-012`.

---

#### P7-TC-005 — Provenance, Artifact, And Observability

**Layer:** L1 / governance / audit

**Depends on:** `P7-TC-004`

**Goal:** Persist score-matrix evidence and stamp complete robust-selection
provenance into governance records and human-readable reporting.

**Deliverables:**

```text
PolicyChangeLedgerEntry robust fields
score matrix artifact handle
human-readable robust selection narrative
README/spec/architecture updates
```

**Tests:**

```text
ledger entry contains objective, params, scenario set, counts, and matrix handle
artifact handle points to persisted matrix
single-candidate fallback has no false robustness claim
adequacy fallback narrative names missing categories
legacy ledger entries remain valid with null/blank robust fields when robust selection is disabled
documentation guard references Phase 7 robust selection evidence
```

**Acceptance:** `P7-ROBUST-010`, `P7-ROBUST-011`, `P7-ROBUST-012`, `X-AUDIT-001`, `X-OBS-001`, `X-DOC-001`.

---

#### P7-TC-006 — Phase 7 Completion Report

**Layer:** documentation

**Depends on:** all previous Phase 7 task cards

**Goal:** Produce an honest completion report with implemented ACs, deferred
work, known limitations, and reproduction commands.

**Deliverables:**

```text
docs/PHASE_7_COMPLETION_REPORT.md
src/test/java/com/nitroj/adaptive/quantum/sor/docs/Phase7CompletionReportTest.java
```

**Acceptance:** `X-DOC-001`.

---

## 7.12 Acceptance Criteria

| ID | Criterion |
|---|---|
| `P7-ROBUST-001` | The optimizer coordinator can emit a `PolicyCandidateSet` of at least two individually valid candidates, pairwise distinct by canonical hash, over a deterministic configured parameter grid. |
| `P7-ROBUST-002` | When only one candidate can be produced, the system may publish it but stamps provenance `objective=SINGLE_CANDIDATE_FALLBACK` and does not assert a robustness objective. |
| `P7-ROBUST-003` | `ScenarioSweepEvaluator` produces a `ScoreMatrix` covering every candidate x scenario pair using `ScenarioScorecardV1`. |
| `P7-ROBUST-004` | Re-running the sweep with identical candidate set, scenario set, and seed yields a bit-identical `ScoreMatrix`. |
| `P7-ROBUST-005` | Each robust objective is a pure function of `ScoreMatrix`, exactly unit-tested with hand-constructed matrices and lowest-`candidateId` tie-breaking. |
| `P7-ROBUST-006` | The active objective and parameters are resolved from configuration at publication time; `CVAR_K` with `k=10` is the default; `MIN_MAX` requires explicit opt-in. |
| `P7-ROBUST-007` | The L0 hot path is unchanged: route execution consumes exactly one immutable published policy, with no scenario or robust-selection dependency. |
| `P7-ROBUST-008` | If scenario adequacy fails, fallback mode selects by `EXPECTED`, stamps `objective=ADEQUACY_FALLBACK`, records missing categories, and emits warning evidence. |
| `P7-ROBUST-009` | Strict adequacy mode blocks publication and exposes `robust_adequacy` plus missing categories on the publication result. |
| `P7-ROBUST-010` | Every robust publication with more than one candidate persists a complete candidate x scenario `ScoreMatrix` artifact retrievable through the governance artifact store. |
| `P7-ROBUST-011` | Every robust publication governance record carries objective, objective parameters, `scenarioSetId`, `scenarioSetVersion`, `scenarioCount`, `candidateCount`, adequacy status, and score-matrix handle. |
| `P7-ROBUST-012` | With `robustSelection.enabled=false`, all existing Phase 1-6 publication, optimizer, scenario, notebook, native, and L0 execution tests pass without behavioral changes or required fixture updates. |
| `X-DET-001` | Phase 7 introduces no nondeterminism into policy selection or scenario replay. |
| `X-AUDIT-001` | Robust selection artifacts and provenance are auditable. |
| `X-OBS-001` | Operator-facing narratives explain why the selected policy won under the configured objective and scenario set. |
| `X-DOC-001` | Completion report and architecture/spec docs state implemented ACs and limits honestly. |

---

## 7.13 Testing Strategy

Phase 7 tests are split by what can be proven exactly and what can only be made
inspectable.

Exactly testable:

```text
objective math
tie-breaking
config validation
candidate distinctness
single-candidate fallback
adequacy fallback and strict block
scorecard formula
CSV serialization
```

Determinism tests:

```text
same candidate set + scenario set + seed => identical ScoreMatrix
same matrix + objective config => identical RobustSelection
```

Integration tests:

```text
RobustPublicationGate publishes the selected winner through PolicyPublisher
failed adequacy strict mode leaves active policy untouched
ledger and artifact handles are populated
robust disabled mode uses the existing single-candidate publication path
L0 route execution tests remain unchanged
```

Regression profile required before Phase 7 completion:

```bash
scripts/run_tests.sh all
```

Focused regression groups that must remain green:

```text
com.nitroj.adaptive.quantum.sor.execution.*
com.nitroj.adaptive.quantum.sor.policy.publication.*
com.nitroj.adaptive.quantum.sor.optimizer.PolicyOptimizerCoordinatorTest
com.nitroj.adaptive.quantum.sor.scenario.*
com.nitroj.adaptive.quantum.sor.optimizer.batch.*
com.nitroj.adaptive.quantum.sor.optimizer.ising.*
```

Characterized, not "proven correct":

```text
scenario-dependent robustness outcome
```

There is no universal ground truth for the robust policy. Tests must assert that
the selected candidate is the correct argmax for the persisted matrix and
objective, not that the scenario set represents every future market.

---

## 7.14 Known Limitations And Deferred Work

1. **Robustness is relative to the declared scenario catalog.** Phase 7 makes
   the objective explicit but does not prove the scenario set is representative.
2. **No scenario generation or adversarial expansion.** Deferred as
   `PLANNED P7-FUT-001`.
3. **Single objective per decision.** Multi-objective/Pareto selection is
   deferred as `PLANNED P7-FUT-002`.
4. **Distributional or chance-constrained objectives beyond CVaR/min-regret**
   are deferred as `PLANNED P7-FUT-003`.
5. **Candidate richness is grid-based.** Diversity-driven candidate generation
   is deferred as `PLANNED P7-FUT-004`.
6. **Initial scorecard is simple by design.** It is deterministic and auditable,
   but not a production transaction-cost-analysis model.

---

## 7.15 Recommended Task Execution Order

```text
P7-TC-001  Candidate-set optimizer contract
P7-TC-002  Scenario sweep evaluator and score matrix
P7-TC-003  Robust objectives and selector
P7-TC-004  Robust publication gate and adequacy
P7-TC-005  Provenance, artifact, and observability
P7-TC-006  Completion report
```

The candidate-set contract gates all downstream work. The selector is small once
the matrix exists. Adequacy and provenance make the robustness claim honest.

---

## 7.16 Glossary Additions

- **Robust selection** — choosing the published policy by a configured
  aggregation of candidate scores across a declared scenario set.
- **CVaR@k** — mean of the worst `k%` of a candidate's scenario scores.
- **Min-regret** — selecting the candidate whose maximum shortfall versus the
  best-per-scenario candidate is smallest.
- **Score matrix** — persisted candidate x scenario evidence used for robust
  objective math and audit.
- **Scenario-set adequacy** — configured minimum scenario count and category
  coverage below which a robustness objective may not be asserted.
- **Adequacy fallback** — degradation to expected-score selection with stamped
  provenance when the scenario set is inadequate and strict mode is disabled.

---

## 7.17 Open Questions For Implementation Kickoff

1. Confirm when demo config should opt in with `robustSelection.enabled=true`.
   The repository default remains `false` to protect Phase 1-6 behavior.
2. Confirm the default robust scenario tag. Recommended: add/curate scenarios
   tagged `robust`, while preserving category-based adequacy.
3. Confirm whether the first candidate grid should perturb tactical weights
   only, or both strategic subset bounds and tactical risk scales.
4. Confirm whether robust selection artifacts should remain under `build/` for
   local proof-of-concept or move to a checked evidence directory for demos.
