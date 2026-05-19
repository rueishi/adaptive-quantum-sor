# Phase 6 Completion Report

Phase 6 adds cross-parent batch venue allocation: a warm-path optimizer that
allocates multiple concurrent parent orders jointly across venues. The model is
intentionally quadratic, because the hard routing cases are created by
self-impact, shared capacity, participation caps, and correlated
information-leakage between parent orders and venues.

## Implemented Scope

- `BatchAllocationProblem` represents parent x venue allocation quantities,
  parent requirements, venue capacity, participation caps, linear venue costs,
  same-venue parent coupling, correlated venue leakage, and optimizer lineage.
- `DeterministicBatchVenueAllocator` is the exact reference solver for small
  problems. It enumerates feasible unit allocations deterministically and
  optimizes the full quadratic objective.
- `BatchAllocationBackend` and `BackendBatchVenueAllocator` provide the
  swappable backend boundary for future cuOpt, QUBO/Ising, CUDA-Q, or native
  implementations while validating each backend result against the reference
  solver on small problems.
- `BatchAllocatorNativeBridge` is the native boundary placeholder. It fails
  closed when unavailable and can delegate to an available backend.
- `BatchAllocationPlanStore` and `BatchAllocationCoordinator` approve only
  feasible batch plans, preserve the latest approved plan after failed
  attempts, and expose the latest applicable plan by optimizer input snapshot.
- `PolicyOptimizationInput` can carry the latest approved batch allocation plan
  so the warm-path output can be consumed without entering the execution hot
  path.
- User-readable optimizer-policy scenarios now cover same-venue self-impact,
  shared capacity, correlated venue leakage, and infeasible fallback behavior.
- `BatchAllocationReport` renders a user-facing Markdown report with objective,
  constraint, fallback, and parent x venue quantity evidence.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P6-BATCH-001 batch allocation model represents concurrent parents
P6-BATCH-002 single-parent compatibility
P6-BATCH-003 quadratic self-impact changes allocation
P6-BATCH-004 shared venue capacity is enforced
P6-BATCH-005 correlated venue leakage is quadratic
P6-BATCH-006 backend boundary and reference equivalence
P6-BATCH-007 off-hot-path safety
P6-BATCH-008 audit and explainability
P6-BATCH-009 failure safety
P6-BATCH-010 scenario and report evidence
```

Planned ACs:

```text
none
```

Failed ACs:

```text
none
```

## Scenario Evidence

Phase 6 scenario files live under `scenarios/optimizer-policy/`:

```text
batch_same_venue_self_impact.yaml
batch_shared_capacity.yaml
batch_correlated_venue_leakage.yaml
batch_infeasible_fallback.yaml
```

These scenarios are intentionally hard for independent per-parent venue ranking:

```text
same-venue self-impact changes allocation versus independent routing
shared capacity forces cross-parent diversification
correlated venue leakage changes allocation versus independent routing
fallback behavior when the batch problem is infeasible
```

## Reproduction Commands

```bash
./gradlew test --tests com.nitroj.adaptive.quantum.sor.optimizer.batch.*
./gradlew test --tests com.nitroj.adaptive.quantum.sor.scenario.ScenarioDefinitionLoaderTest
./gradlew test --tests com.nitroj.adaptive.quantum.sor.docs.Phase6CompletionReportTest
scripts/run_tests.sh scenario
scripts/run_tests.sh all
```

## Known Limits

- The Phase 6 backend is a deterministic reference implementation plus a
  guarded native/backend seam. Production cuOpt, QUBO/Ising, and QPU backends
  are future integrations behind the same boundary.
- The model uses fixed unit quantities for deterministic reference enumeration.
  Large production batches should be solved by a scalable backend rather than
  the exhaustive reference solver.
- Live execution still routes one parent intent at a time; Phase 6 publishes
  warm-path plans for later policy consumption and does not move allocation
  work into the hot execution path.
