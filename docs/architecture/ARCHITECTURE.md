# Architecture

Adaptive Quantum SOR is a Java-first implementation of a policy-driven smart
order router. The system is organized around a zero-dependency public API,
an embedded engine implementation, simulator components that satisfy the public
SPI, explicit transports, and a standalone server assembly.

The current source tree keeps the clean embedded-engine package in
`com.nitroj.sor.core`. Legacy-style scenario replay and notebook scenario API
compatibility now live in `sor-test-server` under
`com.nitroj.adaptive.quantum.sor.*`, outside the reusable core boundary.

## Module Boundaries

```text
sor-api                    Public engine contract, DTOs, events, and SPI.
sor-core                   Embedded engine, policy, execution, and core abstractions.
sor-test-server             SPI-facing simulator adapters, scenario fixtures, and sample server.
sor-transport-aeron        Low-latency remote transport over SBE/Aeron.
sor-transport-http-control Research and ops HTTP control plane.
sor-optimizers-native      Panama native optimizer linkers; no project deps.
sor-client-java            Java remote client surface.
sor-client-python          Python/Jupyter control and research helpers.
```

These boundaries are executable, not only descriptive. Gradle dependency edges
are checked by `ModuleDependencyGraphTest`, public API leakage is checked by
`SorApiZeroDependencyTest`, and package-level architecture rules are checked by
ArchUnit tests in `sor-core` and `sor-test-server`.

## Runtime Layers

```text
Jupyter/Python control plane
HTTP API and lifecycle stream
simulation, feature, and stats state
ML signal and optimizer input state
strategic optimizer
cross-parent batch allocator
tactical optimizer
policy lint, compile, robust select, validate, publish
CPU SOR execution
audit, metrics, and reports
```

The HTTP and Jupyter pieces are control-plane only. They are useful for demos,
research, and observability, but they are not part of the execution hot path.
The Java engine owns runtime state. The simulator-backed sample server starts
through `SimulatorServerApplication` and the `sor-transport-http-control` server;
legacy notebook/scenario compatibility endpoints are owned by `sor-test-server`.

## Simulation Layer

The simulator layer is the canonical replacement for external venues, brokers,
market data, and order flow in Adaptive Quantum SOR. Baseline Phase 1 simulators are seeded
and deterministic. The stateful stochastic/regime simulator upgrade keeps that
replay property while adding persistent market state, correlated venue quotes,
regime-conditioned volatility/spread/liquidity, venue profiles, stale feeds,
outages, halts, and market-state-dependent venue outcomes.

The simulator is Java-first and portable. GPU, CUDA-Q, Ising, cuOpt, cuRAND, or
StyleGAN-style image generators are not required for canonical market-data
generation.

Low-level simulator classes live under:

```text
com.nitroj.sor.sim.adapters
com.nitroj.sor.sim.scenario
com.nitroj.sor.sim.scenario.venues
```

Scenario orchestration classes live in `sor-test-server` under:

```text
com.nitroj.adaptive.quantum.sor.scenario
```

This separation is intentional. `sim` classes generate bounded state for one
domain, while `scenario` classes define replayable scripts, simulated time,
independent random streams, summaries, and assertions for Gradle/JUnit scenario
tests.

Live Jupyter scenario testing uses the same scenario concepts but a different
state boundary. Gradle/JUnit scenarios create an isolated `ScenarioEngineContext`
per run and discard it. Jupyter/API scenarios must use an explicit
user-requested reset mode such as `ISOLATED`, `PURGE_AND_REPOPULATE`,
`KEEP_POLICY_PURGE_STATS`, or `APPEND`. Any purge of live engine state must
emit a visible reset summary and lifecycle/audit evidence before scenario
results are shown.

## Policy Flow

`PolicyOptimizationInput` snapshots market, venue, stats, metadata, model, risk,
and policy lineage. Strategic optimization selects venue subsets per route.
Tactical optimization then tunes numeric policy parameters inside those
subsets. The compiler creates an immutable `SorPolicy`, lint and validation
guard it, and `PolicyPublisher` atomically swaps the active policy when the
publication gate allows it.

## Route Identity

The system uses dense IDs and deterministic array layout. Route-level state is
addressed by:

```text
routeKey = ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId
```

Venue-level policy values use instrument x venue x regime x urgency indexing.
This keeps Adaptive Quantum SOR easy to reason about and keeps future native boundaries
straightforward.

## Strategic And Tactical Optimizers

Phase 1 provides deterministic Java stubs. Phase 2 adds a C++/CUDA-aware
tactical native build and JNI boundary. Phase 3 adds route-level QUBO/Ising
strategic formulation, a C++ strategic backend artifact, approved strategic
result storage, fallback/health handling, and strategic audit lineage.

Approved strategic results are stored separately from failed attempts so the
tactical optimizer consumes the latest approved result, not merely the latest
attempted result.

## Cross-Parent Batch Allocation

Phase 6 adds a warm-path batch allocator between strategic venue selection and
tactical policy/execution. It optimizes parent x venue quantities across
multiple active parent orders at once, then publishes only validated
`BatchVenueAllocationPlan` records.

The allocation objective is deliberately quadratic:

```text
linearCost[p,v] for parent p using venue v
sameVenuePairCost[p,q,v] when two parents use the same venue
venueCorrelationPairCost[v,w] when parents use correlated venues
```

Those pair terms represent the hard cases that independent per-parent routing
cannot express: self-impact, shared venue capacity, aggregate participation
caps, and correlated information leakage. Small problems are solved by the
deterministic reference allocator; larger future integrations can plug in
cuOpt, QUBO/Ising, CUDA-Q, or native backends behind `BatchAllocationBackend`.

The batch allocator is not part of L0. If a backend is unavailable, times out,
or returns an invalid plan, `BatchAllocationPlanStore` keeps the latest approved
plan and execution continues with the existing route-level policy.

## Robust Policy Selection

Phase 7 adds an opt-in publication-gate wrapper for robust policy selection.
`PolicyCandidateSet` carries deterministic candidate policies from the optimizer
coordinator to L1. Core owns the `ScenarioSweepEvaluator` interface and
primitive `ScenarioSetDescriptor`; `sor-test-server` supplies the concrete
scenario runner evaluator that records a `ScoreMatrix`. Pure objectives such as
`CVAR_K`, `MIN_MAX`, `EXPECTED`, and `MIN_REGRET` select one candidate. `RobustPublicationGate` then
delegates the winner to the existing `PolicyPublisher.publish(...)` path.

The default configuration keeps robust selection disabled, preserving the Phase
1-6 single-candidate publication flow. When enabled, the score matrix handle,
scenario-set provenance, objective, adequacy status, and candidate count are
stamped into the policy ledger. L0 continues to read exactly one immutable
published `SorPolicy`.

## Python And Jupyter

Repository-local Python support is split so production clients stay clean:
`tools/notebook-helpers/adaptive_quantum_sor_notebooks` owns notebook widgets,
report templates, and control-plane calls to the running Java engine, while
`tools/python-research/adaptive_quantum_sor_research` owns research schemas,
dataset helpers, and scenario catalog access:

```text
read_feature_dataframe
write_feature_dataframe
write_feature_dataset
write_model_predictions_artifact
SorNotebookClient
```

`SorNotebookClient` sends REST-style requests to `http://127.0.0.1:<port>`.
It does not start a separate engine and does not own Java runtime state.

Jupyter users can load `tools/python-research/examples/sor_notebook_features_large.csv` as a
DataFrame, perform venue/regime analysis, train models through
`tools/python-research/scripts/train_models.py`, and write Java-importable prediction artifacts. The
artifact contract is:

```text
predictions.csv
model_metadata.properties
feature schema version
prediction SHA-256 checksum
```

Invalid model artifacts must not replace prior approved signals.

## Native Build

Gradle owns the native build. `nativeBuild` invokes CMake, `nativeTest` invokes
CTest, and `check` depends on native tests. This keeps Java, C++, CUDA-aware
tests, integration tests, and documentation guards under one repeatable command.

## Documentation And Evidence

Phase completion reports in `docs/` capture implemented acceptance criteria,
known limitations, and validation evidence. Documentation tests keep these
artifacts from drifting away from the code and scripts they describe.
