# Sequence Diagrams

These Mermaid diagrams document the core Adaptive Quantum SOR flows across routing,
optimization, publication, notebooks, and venue outcomes.

The editable professional diagram source is:

```text
docs/sequence_diagrams.drawio
```

Rendered PNGs:

```text
docs/sequence_parent_order_routing.png
docs/sequence_policy_optimization_cycle.png
docs/sequence_cross_parent_batch_allocation.png
docs/sequence_robust_policy_selection.png
docs/sequence_policy_publication.png
docs/sequence_jupyter_order_submission.png
docs/sequence_live_jupyter_scenario_parent_orders.png
docs/sequence_venue_behavior_outcome_loop.png
```

## Parent Order Routing

![Parent order routing](sequence_parent_order_routing.png)

```mermaid
sequenceDiagram
    participant User as Trader or Simulator
    participant API as SorHttpApiServer
    participant Queue as ParentOrderIntentQueue
    participant SOR as PolicyDrivenSorExecutioner
    participant Policy as PolicyPublisher
    participant Audit as RouteAuditWriter

    User->>API: POST /orders
    API->>Queue: offer(OrderIntent)
    API-->>User: OrderStatusView
    SOR->>Queue: poll()
    SOR->>Policy: activePolicy()
    SOR->>SOR: route by HotRouteBook routeKey
    SOR->>Audit: append(RouteAuditEvent)
```

## Policy Optimization Cycle

![Policy optimization cycle](sequence_policy_optimization_cycle.png)

```mermaid
sequenceDiagram
    participant Input as PolicyOptimizationInput
    participant ML as ModelSignalState
    participant Strategic as CudaQStrategicOptimizer
    participant Tactical as CudaTacticalOptimizer
    participant Candidate as MutablePolicyCandidate

    Input->>ML: read approved model signals
    Input->>Strategic: optimize(input)
    Strategic-->>Input: StrategicVenueSubsetResult
    Input->>Tactical: optimize(strategic, input)
    Tactical-->>Candidate: TacticalPolicyResult
    Tactical->>Candidate: apply weights, penalties, limits
```

## Cross-Parent Batch Venue Allocation

![Cross-parent batch venue allocation](sequence_cross_parent_batch_allocation.png)

```mermaid
sequenceDiagram
    participant Trigger as Warm-path Trigger
    participant Snapshot as Parent/Market Snapshot
    participant Problem as BatchAllocationProblem
    participant Backend as BatchAllocationBackend
    participant Reference as DeterministicBatchVenueAllocator
    participant Store as BatchAllocationPlanStore
    participant Input as PolicyOptimizationInput
    participant Report as BatchAllocationReport

    Trigger->>Snapshot: collect active parent orders and venue state
    Snapshot->>Problem: build parent x venue quantities, capacities, pair costs
    Problem->>Reference: solve small deterministic reference
    Problem->>Backend: optional native/cuOpt/QUBO allocation
    Backend-->>Problem: plan or failure status
    Problem->>Problem: validate feasibility and objective
    alt backend unavailable, timeout, or invalid
        Problem-->>Store: keep latest approved plan
    else valid allocation
        Problem->>Store: approve(BatchVenueAllocationPlan)
        Store->>Input: attach latest applicable plan by inputSnapshotId
        Store->>Report: render objective, constraints, fallback, quantities
    end
```

## Robust Policy Selection

![Robust policy selection](sequence_robust_policy_selection.png)

```mermaid
sequenceDiagram
    participant Coordinator as PolicyOptimizerCoordinator
    participant Candidates as PolicyCandidateSet
    participant Gate as RobustPublicationGate
    participant Evaluator as ScenarioSweepEvaluator
    participant Runner as ScenarioRunner
    participant Store as ScoreMatrixArtifactStore
    participant Objective as RobustObjective
    participant Publisher as PolicyPublisher
    participant Ledger as PolicyChangeLedger

    Coordinator->>Candidates: build candidate policies from optimizer results
    Candidates->>Gate: submit candidate set, scenario descriptor, objective config
    alt robust selection disabled
        Gate->>Publisher: publish single compiled candidate through existing gate
    else scenario set inadequate and strict mode
        Gate-->>Coordinator: rejected PublicationGateResult with adequacy evidence
    else robust selection enabled
        Gate->>Gate: validate scenario-set adequacy
        Gate->>Evaluator: evaluate candidates x scenarios
        Evaluator->>Runner: replay declared scenario set
        Runner-->>Evaluator: ScenarioSummary values
        Evaluator-->>Gate: deterministic ScoreMatrix
        Gate->>Store: persist score matrix artifact
        Gate->>Objective: select winner from score matrix
        Objective-->>Gate: RobustSelection
        Gate->>Publisher: publish selected SorPolicy
        Publisher->>Ledger: annotate robust selection provenance
        Publisher-->>Coordinator: accepted PublicationGateResult
    end
```

## Policy Publication

![Policy publication](sequence_policy_publication.png)

```mermaid
sequenceDiagram
    participant Candidate as MutablePolicyCandidate
    participant Lint as PolicyLint
    participant Compiler as PolicyCompiler
    participant Validator as PolicyValidator
    participant Publisher as PolicyPublisher
    participant Store as PolicySnapshotStore

    Candidate->>Lint: lint(candidate, input, strategic)
    Candidate->>Compiler: compile(candidate, strategic, tactical, lint)
    Compiler-->>Validator: SorPolicy
    Validator-->>Publisher: PolicyValidationReport
    Publisher->>Publisher: publication gate
    Publisher->>Store: store(policy)
    Publisher-->>Publisher: activePolicy atomic swap
```

## Jupyter Order Submission

![Jupyter order submission](sequence_jupyter_order_submission.png)

```mermaid
sequenceDiagram
    participant Notebook as Jupyter Notebook
    participant Client as SorNotebookClient
    participant API as SorHttpApiServer
    participant Queue as ParentOrderIntentQueue
    participant Events as LifecycleEventStore

    Notebook->>Client: submit_orders_dataframe(DataFrame)
    Client->>API: POST /orders
    API->>Queue: offer(OrderIntent)
    API->>Events: append(order accepted)
    API-->>Client: OrderStatusView JSON
    Client-->>Notebook: pandas DataFrame
```

## Live Jupyter Scenario Run With Explicit Reset And Parent Orders

![Live Jupyter scenario run with explicit reset and parent orders](sequence_live_jupyter_scenario_parent_orders.png)

```mermaid
sequenceDiagram
    participant Notebook as Jupyter Notebook
    participant Catalog as Scenario Catalog Library
    participant Client as SorNotebookClient
    participant API as Scenario API
    participant Control as ScenarioControlService
    participant Context as ScenarioEngineContext
    participant Runner as ScenarioRunner
    participant SOR as PolicyDrivenSorExecutioner
    participant Venue as VenueBehaviorSimulator
    participant Events as LifecycleEventStore

    Notebook->>Catalog: load/search/suggest scenario
    Catalog-->>Notebook: scenario metadata + parent order suggestions
    Notebook->>Client: reset_scenario(resetMode)
    Client->>API: POST /scenario/reset
    API->>Control: reset(ScenarioResetRequest)
    Control->>Context: clear/keep/repopulate by resetMode
    Control->>Events: append scenario reset summary
    API-->>Client: ScenarioResetSummary JSON
    Client-->>Notebook: reset summary DataFrame
    Notebook->>Client: run_scenario(ScenarioSpec, parentOrders[])
    Client->>API: POST /scenario/run
    API->>Control: run(ScenarioRunRequest parentOrders[])
    Control->>Runner: run(spec, context)
    Runner->>SOR: submit parentOrders[] at scheduled ticks
    SOR->>Venue: route child orders by active HotRouteBook
    Venue-->>Runner: fills/rejects/residuals
    Runner-->>Control: ScenarioSummary + parent-order evidence
    Control->>Events: append scenario run summary
    API-->>Client: ScenarioRunResult JSON
    Client-->>Notebook: summary/events/order-results DataFrames
```

## Venue Behavior Outcome Loop

![Venue behavior outcome loop](sequence_venue_behavior_outcome_loop.png)

```mermaid
sequenceDiagram
    participant SOR as SOR Executioner
    participant Child as ChildOrderState
    participant Venue as VenueBehaviorSimulator
    participant Outcomes as ExecutionOutcomeStore
    participant Stats as FeatureAggregator
    participant Features as VenueStatsState

    SOR->>Child: create child orders
    Child->>Venue: simulated venue interaction
    Venue->>Outcomes: append ACK/FILL/REJECT
    Outcomes->>Stats: rolling aggregation
    Stats->>Features: update latency/fill/toxicity/slippage
```
