# Adaptive Quantum SOR — Phase 9 Specification: Engine-Owned State And Real-Path Scenario Replay

> **Integration note for the maintainer.** This document is ready to append to
> `adaptive_quantum_sor_spec_v1.md` as Phase 9. Phase 8 originally listed a
> C++ L0 port as the Phase 9 placeholder. That work remains deferred. The
> production-critical successor to Phase 8 is now the state-ownership correction
> described here: the SOR must own the routing snapshots it uses, and scenario
> testing must exercise the same path a real OMS/EMS integration would use.

---

## 9.0 Document Control

| Field | Value |
|---|---|
| Phase | 9 |
| Phase title | Engine-Owned State And Real-Path Scenario Replay |
| Status | PLANNED |
| Prerequisite phases | Phase 8 COMPLETE |
| Supersedes roadmap note | Phase 8's deferred "Phase 9 C++ L0 port" placeholder. C++ L0 remains deferred until engine-owned state and replay semantics are correct. |
| Owning layers | `sor-api`, `sor-core`, `sor-test-server`, `sor-transport-aeron`, `sor-transport-http-control`, `sor-codec`, `sor-client-java`, `sor-client-python` |
| Hot-path impact | Must preserve Phase 8 hot-path constraints. New reset, diagnostics, scenario replay, and snapshot APIs are control-plane only. |
| Backward compatibility | Existing simulator-local tests may remain, but end-to-end scenario evidence must move to the real SOR path. Public SPI changes require explicit API/ABI compatibility tests and migration notes. |

### 9.0.1 Change Notes

```text
0.1 — Initial Phase 9 draft. Captures the state-ownership design correction:
the engine must maintain its own market book and working order book, market
data must be venue-aware, scenario reset must be explicit and audited, and the
test server must stop using simulator-local books as the main proof of SOR
routing.
```

---

## 9.1 Objective

Phase 8 made the SOR embeddable and pluggable, but it left an important
integration gap: the simulator can drive scenario outcomes from simulator-owned
state while the active `SorEngineImpl` does not yet ingest market data into an
engine-owned market book. That is acceptable for simulator unit tests and not
acceptable as production evidence.

Phase 9 makes the engine own the state it routes from:

```text
OMS/EMS or algo layer
  supplies parent-order intent and, on recovery, order-state snapshots

MarketDataSource
  supplies venue-aware market updates

SorEngineImpl
  copies those updates into SOR-owned MarketBookState
  maintains SOR-owned active parent/child order state
  routes from those internal snapshots

VenueAdapter
  receives child orders through ring writers
  returns fills/rejects through callbacks

ScenarioRunner
  controls deterministic inputs but exercises the same path
```

The end state is simple: scenario tests may still generate artificial market
data, but they must not bypass the engine's own market book or order state when
claiming SOR correctness.

### 9.1.1 Why This Phase Exists

The current test-server path is simulator-driven:

```text
ScenarioRunner -> SimulatedMarketDataSource.currentBook()
               -> SimulatedVenueAdapter.process(...)
```

That makes scenarios easy to run, but it skips the most important production
chain:

```text
MarketDataSource -> onQuote() -> MarketBookState -> route decision
```

The current `Quote` carrier also lacks `venueId`, so it cannot faithfully update
a per-venue market book. A real SOR routes by comparing venues; market data is
not just instrument-scoped.

### 9.1.2 What Is And Is Not Provable

Phase 9 proves:

```text
the SOR owns the market book it routes from
the SOR owns a working internal order book for active routing state
scenario market data can be purged and repopulated deterministically
scenario orders go through SorEngine, VenueAdapter, and callbacks
reset behavior is explicit, mode-specific, safety-gated, and audited
simulator-local shortcuts are limited to simulator unit tests
```

Phase 9 does not prove:

```text
production exchange connectivity
full OMS/EMS implementation
full market-data plant behavior
C++ L0 routing
multi-tenant hosting
```

---

## 9.2 Scope

### 9.2.1 In Scope

1. Venue-aware market data SPI redesign.
2. SOR-owned market book ingestion and lifecycle.
3. SOR-owned internal order state for active parent/child routing.
4. Control-plane reset/purge/seed API for scenario and recovery flows.
5. Venue adapter integration from engine child-order rings to simulator fills.
6. Real-path scenario execution through `SorEngine`.
7. Safe diagnostics for market/order/reset/replay verification.
8. Audit and replay evidence for reset, market ingestion, route, child, fill,
   reject, and policy events.
9. Clear API boundaries between hot-path `SorEngine` and control-plane
   operations.
10. Updated tests proving the above.
11. A built-in optional HTTP control module for users who embed SOR, separate
    from simulator/test-server scenario endpoints.
12. Production-grade JDK refresh with Compact Object Headers enabled with ZGC
    after regression and benchmark evidence.
13. Startup hydration from OMS/EMS order-state snapshots and market-data
    snapshots before the engine accepts new parent intent.

### 9.2.2 Out Of Scope

- C++ L0 port. Deferred until Java engine state ownership is correct.
- Real exchange adapters beyond simulator/reference adapters.
- OMS/EMS product implementation. Phase 9 defines what the SOR receives from
  OMS/EMS, not an OMS/EMS itself.
- Synchronous OMS/EMS or market-data lookups on the routing path.
- Destructive production reset without safety gates.
- Placing HTTP server code inside `sor-core`; HTTP remains an optional
  transport/control module over `sor-api` and `sor-core`.
- Treating notebook scenario endpoints as production user endpoints.

---

## 9.3 Architecture Target

### 9.3.1 Ownership Model

```text
OMS/EMS
  book of record for parent/client orders
  sends startup/recovery order-state snapshots before new parent intent
  sends new parent orders only after the SOR reports ready

Market data plant/feed
  source of truth for market data
  publishes startup market snapshots and venue-aware incremental updates

SOR
  owns local low-latency market book used for routing
  owns working parent/child order state used for routing
  owns route decisions, active policy identity, and replay evidence

Venue adapters/gateways
  own venue protocol/session truth
  receive child orders and return acks/fills/rejects/cancels
```

The SOR's market book and order book are derived working state. They are not the
firm's golden record, but they are the only state the hot routing path may read.

### 9.3.2 Startup Hydration Flow

Before the SOR accepts new parent-order intent from OMS/EMS, it must build a
consistent local routing view from explicit startup inputs:

```text
Construct engine
  -> subscribe to market-data source
  -> load or replay initial market snapshot into MarketBookState
  -> load OMS/EMS active parent/child state into EngineOrderBook
  -> validate dimensions, sequence, freshness, and reconciliation evidence
  -> mark engine READY only after hydration succeeds
  -> accept new ParentOrderRequest from OMS/EMS
```

The hot path must never synchronously query OMS/EMS or the market-data plant to
fill missing state. Startup hydration is a control-plane lifecycle phase, and
its success or failure must be visible through diagnostics and audit evidence.

### 9.3.3 Real-Path Scenario Flow

```text
Scenario reset
  -> purge selected SOR runtime state
  -> clear MarketBookState and internal order state
  -> preserve or reload policy according to reset mode

Scenario market setup
  -> SimulatedMarketDataSource.generateTick(...)
  -> publish venue-aware Quote
  -> SorEngineImpl.onQuote(...)
  -> MarketBookState.updateTopOfBook(...)

Scenario order flow
  -> ScenarioRunner submits ParentOrderRequest to SorEngine
  -> SorEngine routes from MarketBookState and active policy
  -> SorEngine writes ChildOrderRef to VenueAdapter ring
  -> SimulatedVenueAdapter polls the ring
  -> simulated venue emits FillReport/RejectReport callback
  -> SorEngine updates internal order state and emits SorEvent
```

Simulator-local direct calls such as `marketData.currentBook()` remain valid for
testing simulator formulas, but they are not valid as the main SOR end-to-end
evidence path.

---

## 9.4 Required Work Items

### P9-TC-001 — Venue-Aware Market Data SPI

The public SPI must model market data by instrument and venue.

Minimum required change:

```java
public final class Quote {
    private int instrumentId;
    private int venueId;
    ...
}
```

Recommended subscription shape:

```java
void subscribe(int instrumentId, int venueId, MarketDataListener listener);
void unsubscribe(int instrumentId, int venueId, MarketDataListener listener);
```

Compatibility may retain instrument-only subscription as a default/broadcast
mode during migration.

Acceptance criteria:

```text
P9-SPI-001 Given a Quote, then it carries both instrumentId and venueId.
P9-SPI-002 Given venue-aware subscriptions, when a source publishes venue N,
           then only matching listeners or documented wildcard listeners fire.
P9-SPI-003 Given existing simulator sources, when migrated, then every generated
           top-of-book update identifies the originating venue.
P9-SPI-004 Given public API signature changes, then API/ABI compatibility tests
           document the break or prove compatibility.
```

### P9-TC-002 — SOR-Owned Market Book

`SorEngineImpl` must allocate and own a `MarketBookState` sized from engine
configuration. Market data callbacks must copy primitive quote fields into that
book. The engine must not retain reusable `Quote` objects.

Acceptance criteria:

```text
P9-MD-001 Given a venue-aware quote, when onQuote receives it, then the matching
          MarketBookState instrument/venue cell is updated.
P9-MD-002 Given a reusable Quote object, when the source mutates it after the
          callback, then the engine-owned market book remains stable.
P9-MD-003 Given invalid crossed or negative quote data, when ingestion occurs,
          then the engine rejects, sanitizes, or quarantines it according to a
          documented policy.
```

### P9-TC-003 — SOR-Owned Internal Order Book

The engine must own working state for active parent orders, child orders,
pending child quantity, fills, rejects, cancels, and remaining quantity. OMS/EMS
remains the external book of record, but routing must not depend on synchronous
OMS/EMS reads.

Acceptance criteria:

```text
P9-ORDER-001 Given a new ParentOrderRequest, when submitted, then the engine
             creates a working parent-order state record.
P9-ORDER-002 Given a route decision, when child orders are emitted, then pending
             child quantity is tracked by parent and venue.
P9-ORDER-003 Given FillReport and RejectReport callbacks, when delivered, then
             engine-owned order state and public OrderStatus are updated.
P9-ORDER-004 Given recovery or scenario seeding, when an order-state snapshot is
             applied, then the engine can restore active parent/child state
             without using submitParentOrder as a fake recovery API.
```

### P9-TC-004 — Control-Plane Reset, Purge, And Seed

Add a control-plane API separate from the hot-path `SorEngine` surface. The API
must make reset mode, safety rules, and cleared/kept/repopulated state explicit.

Recommended public shape:

```java
public interface SorControlPlane {
    SorResetSummary reset(SorResetRequest request);
    SorStateSummary stateSummary();
    MarketDataSnapshotSummary marketDataSnapshot();
}
```

Recommended reset modes:

```text
CLEAR_MARKET_DATA
PURGE_RUNTIME_STATE
KEEP_POLICY_PURGE_RUNTIME
PURGE_AND_REPOPULATE
SCENARIO_REPLAY_RESET
RECOVERY_REBUILD
APPEND
```

Acceptance criteria:

```text
P9-RESET-001 Given live parent or child orders and a destructive reset requiring
             quiescence, when reset is requested, then it fails closed.
P9-RESET-002 Given a scenario replay reset, when accepted, then the summary
             names all cleared, kept, and repopulated state.
P9-RESET-003 Given any reset, when completed or rejected, then an audit/lifecycle
             event is emitted.
P9-RESET-004 Given production configuration disables destructive reset modes,
             when requested, then the control plane rejects them.
P9-RESET-005 Given CLEAR_MARKET_DATA or purge mode, when reset is accepted, then
             MarketBookState is empty and sequence state is reset or explicitly
             advanced with reset evidence.
```

### P9-TC-005 — Venue Adapter Real Integration

Execution must emit child orders through `VenueAdapter.childOrderRingWriter`.
The simulator must poll those same rings and return reports through
`VenueAdapterCallback`.

Acceptance criteria:

```text
P9-VENUE-001 Given a route decision, when a child order is emitted, then it is
             offered to the configured venue ring writer.
P9-VENUE-002 Given a full venue ring, when the engine offers a child order,
             then backpressure is recorded and exposed through events/metrics.
P9-VENUE-003 Given simulator venue polling, when a child order is consumed,
             then the subsequent FillReport/RejectReport reaches the engine
             callback path.
```

### P9-TC-006 — Scenario Tests Through The Real Path

The main scenario runner must submit orders through `SorEngine` and consume
events/fills/rejects through the public SPI/callback path. Direct simulator book
access remains only for simulator unit tests.

Acceptance criteria:

```text
P9-SCEN-001 Given any existing scenario YAML, when replayed in real-path mode,
            then market data enters through MarketDataSource and onQuote.
P9-SCEN-002 Given scenario parent orders, when replayed, then they are submitted
            through SorEngine.submitParentOrder.
P9-SCEN-003 Given a venue outcome, when the simulator emits fill/reject
            callbacks, then SorEngine emits matching SorEvent evidence.
P9-SCEN-004 Given a deterministic seed, when the scenario is replayed from a
            purge reset, then checksums and route evidence are deterministic.
```

### P9-TC-007 — Safe Diagnostics

Expose immutable summaries/checksums for control-plane observation. Do not
return mutable internal state.

Diagnostics should include:

```text
market book sequence/checksum
populated market book cell count
last quote epoch nanos
active parent order count
pending child order count
fills/rejects/cancels counters
feed health summary
last reset summary
active policy identity
```

Acceptance criteria:

```text
P9-DIAG-001 Given a populated market book, when diagnostics are read, then the
            caller receives sequence/checksum and counts, not mutable arrays.
P9-DIAG-002 Given reset/replay, when diagnostics are read, then the last reset
            summary and replay-safe flag are visible.
P9-DIAG-003 Given HTTP and Aeron control surfaces, when diagnostics are exposed,
            then both agree with embedded control-plane summaries.
```

### P9-TC-008 — Audit And Replay Evidence

Reset, market ingestion, route decisions, child emissions, fills, rejects,
policy publication, and recovery rebuilds must be auditable.

Acceptance criteria:

```text
P9-AUDIT-001 Given a scenario run, when it completes, then evidence links reset
             summary, market-data checksum, route events, and final order state.
P9-AUDIT-002 Given a rejected reset or rejected quote, when it occurs, then the
             reason is visible in lifecycle/audit evidence.
P9-AUDIT-003 Given replay from the same seed and reset mode, when completed,
             then evidence is deterministic except documented timestamps.
```

### P9-TC-009 — Clarify API Boundaries

Keep `SorEngine` small and hot-path oriented. Put reset, seed, diagnostics,
policy reload, and scenario controls behind a separate control-plane interface
or module.

Acceptance criteria:

```text
P9-API-001 Given public API review, then hot-path order submission remains on
           SorEngine and reset/diagnostics remain control-plane only.
P9-API-002 Given embedded mode, then integrators can access the control plane
           without depending on sor-test-server internals.
P9-API-003 Given out-of-process mode, then control-plane operations are
           available over explicit non-hot-path transport endpoints.
```

### P9-TC-010 — Test Suite Updates

Retain low-level simulator tests, but add real-path integration tests that prove
engine-owned market/order state.

Required test groups:

```text
MarketBookState clear/reset/sequence tests
Quote venueId API/SPI tests
SorEngineImpl onQuote ingestion tests
engine-owned order book tests
control-plane reset safety tests
scenario purge-and-repopulate tests
real-path scenario replay tests
venue ring writer/fill callback tests
diagnostic checksum tests
API/ABI compatibility tests
```

Acceptance criteria:

```text
P9-TEST-001 Given the full Gradle check suite, when run, then simulator-local
            and real-path scenario tests both pass.
P9-TEST-002 Given a scenario test that uses marketData.currentBook() as primary
            SOR evidence, then the test fails architecture ownership checks.
P9-TEST-003 Given API/SPI changes, when compatibility tests run, then the break
            is documented or compatibility is proven.
```

### P9-TC-011 — Built-In User HTTP Control Module

Provide a supported optional HTTP module for users who embed SOR and want a
ready-made operational/control surface. This module must live outside
`sor-core`; it is an adapter over `SorEngine`, `SorControlPlane`,
`Observability`, and optionally persistence/audit readers.

Recommended module ownership:

```text
sor-api
  public Java contracts and DTOs

sor-core
  embedded engine implementation and state ownership

sor-transport-http-control
  built-in user HTTP control server

sor-test-server
  simulator, scenario, notebook demo server, and test fixtures
```

Recommended supported user endpoints:

```text
GET  /healthz
GET  /ready
GET  /metrics
GET  /openapi.json
POST /orders
GET  /orders/{id}
GET  /control/state
GET  /control/market-data
POST /control/reset
GET  /policy/current
```

Acceptance criteria:

```text
P9-HTTP-001 Given a user embeds SorEngine, when they instantiate the built-in
            HTTP control server, then it exposes supported endpoints without
            depending on sor-test-server classes.
P9-HTTP-002 Given POST /orders or GET /orders/{id}, when called, then the
            server delegates only to SorEngine hot/control methods and returns
            documented JSON.
P9-HTTP-003 Given diagnostics/reset endpoints, when called, then the server
            delegates to SorControlPlane and preserves reset safety gates.
P9-HTTP-004 Given /metrics, when called, then the server delegates to
            Observability.prometheusText() or a documented empty response.
P9-HTTP-005 Given /openapi.json, when called, then it documents every built-in
            user endpoint and does not advertise scenario/test-only endpoints.
```

### P9-TC-012 — Separate User HTTP From Test-Server Scenario API

The built-in user HTTP module must not absorb simulator or notebook scenario
responsibilities. Scenario YAML replay, simulator-generated orders, scenario
events, and notebook demo fixtures remain in `sor-test-server` unless a future
optional scenario module is explicitly created.

Endpoints that remain test-server/demo-only:

```text
POST /scenario/reset
POST /scenario/run
GET  /scenario/summary
GET  /scenario/events
GET  /events/stream
```

Acceptance criteria:

```text
P9-HTTP-BOUNDARY-001 Given dependency analysis, then
                     sor-transport-http-control must not depend on
                     sor-test-server or simulator packages.
P9-HTTP-BOUNDARY-002 Given built-in HTTP OpenAPI output, then /scenario/*
                     endpoints are absent.
P9-HTTP-BOUNDARY-003 Given notebook/test-server APIs, then they continue to
                     work through NotebookScenarioHttpServer or an explicitly named
                     demo/scenario server.
P9-HTTP-BOUNDARY-004 Given public docs, then user HTTP endpoints and
                     test-server scenario endpoints are described separately.
```

### P9-TC-013 — User HTTP Client And Notebook Alignment

Notebook helpers should be clear about which server they target. The notebook
client may support both the built-in user HTTP control surface and the
test-server scenario/demo surface, but scenario methods must be documented as
test-server-only.

Acceptance criteria:

```text
P9-HTTP-CLIENT-001 Given SorNotebookClient or any Python helper, then ordinary
                   order/status/metrics/control calls can target the built-in
                   user HTTP module when the endpoint exists.
P9-HTTP-CLIENT-002 Given scenario methods, then docs and method names/comments
                   identify them as test-server/demo-only.
P9-HTTP-CLIENT-003 Given a user points notebooks at the built-in HTTP module,
                   then missing scenario endpoints fail clearly instead of
                   implying the production server should implement them.
P9-HTTP-CLIENT-004 Given README examples, then they distinguish
                   `sor-transport-http-control` from `sor-test-server`.
```

### P9-TC-014 — Built-In HTTP Tests, Docs, And Examples

Add tests and documentation that make the built-in HTTP module a real supported
surface for users, not an accidental copy of the test server.

Required coverage:

```text
HTTP endpoint tests for health/ready/metrics/orders/control reset/diagnostics
OpenAPI contract tests for supported user endpoints
architecture tests preventing sor-test-server imports
example Java bootstrap showing SorEngine + HttpControlPlaneServer
documentation table separating user HTTP endpoints from scenario/demo endpoints
compatibility tests for existing test-server notebook endpoints
```

Acceptance criteria:

```text
P9-HTTP-TEST-001 Given :sor-transport-http-control:check, then built-in user
                 HTTP endpoint tests pass without starting sor-test-server.
P9-HTTP-TEST-002 Given architecture tests, then scenario/simulator packages
                 cannot leak into the built-in HTTP module.
P9-HTTP-TEST-003 Given examples/docs, then a user can start a core-backed HTTP
                 control server without copying test-server code.
P9-HTTP-TEST-004 Given the full Gradle check suite, then both user HTTP module
                 tests and test-server scenario endpoint tests pass.
```

### P9-TC-015 — Extract Reusable Scenario And Simulator Testkit Module

The engine must not know the concept of a scenario. Scenario replay is a
testing, notebook, benchmark, and demo concern that drives the real engine
through public/API-level inputs. Reusable scenario and simulator support should
therefore live outside `sor-core` and outside the runnable `sor-test-server`
application.

Introduce a dedicated module:

```text
sor-testkit
```

The module owns reusable test support packages:

```text
com.nitroj.sor.testkit.scenario.*
com.nitroj.sor.testkit.sim.*
```

Candidate contents:

```text
ScenarioSpec
ScenarioDefinition
ScenarioDefinitionLoader
ScenarioRunner
ScenarioSummary
ScenarioAssertions
ScenarioClock
ScenarioAuditEvidence
Scenario simulator configuration/value objects
Simulated market/risk/venue adapters
ManualClock
InMemoryPersistence
Simulated catalogs and scenario profiles
YAML scenario resources
```

The runnable `sor-test-server` module should keep only server/application
wrapping:

```text
NotebookScenarioHttpServer
NotebookScenarioApiLauncher
SimulatorServerApplication
SimulatorServerMain
HTTP request/response DTOs that are notebook/demo-server-specific
```

Dependency direction:

```text
sor-core -> sor-api
sor-testkit -> sor-api + sor-core
sor-test-server -> sor-api + sor-core + sor-testkit + sor-transport-http-control
```

`sor-api` and `sor-core` must not depend on `sor-testkit`, and no production
engine package may import `com.nitroj.sor.testkit.*`.

Acceptance criteria:

```text
P9-TESTKIT-001 Given Gradle settings, then `sor-testkit` is a standalone module
                  with clear dependencies on `sor-api` and required core
                  implementation modules.
P9-TESTKIT-002 Given architecture tests, then `sor-api`, `sor-core`, and
                  production transport modules cannot depend on
                  `com.nitroj.sor.testkit.*`.
P9-TESTKIT-003 Given package names, then reusable scenario/simulator support is
                  under `com.nitroj.sor.testkit.*`, while runnable HTTP/demo
                  server code remains under `com.nitroj.sor.testserver.*` or
                  `com.nitroj.sor.testserver.*`.
P9-TESTKIT-004 Given documentation, then the distinction between engine,
                  testkit, and runnable test server is explicit.
```

### P9-TC-016 — Migrate Scenario Replay To Testkit And Keep Server Thin

Move reusable scenario and simulator code from `sor-test-server` into
`sor-testkit` without changing the scenario execution model. Scenario replay
must continue to drive the real `SorEngine` path. The move is architectural:
the engine remains scenario-agnostic, and the server becomes a thin wrapper
around reusable testkit services.

Migration scope:

```text
Move reusable scenario classes from com.nitroj.sor.testserver.scenario.*
  to com.nitroj.sor.testkit.scenario.*
Move reusable simulator classes from com.nitroj.sor.sim.adapters.*,
  com.nitroj.sor.sim.scenario.*, and com.nitroj.sor.sim.scenario.venues.*
  to com.nitroj.sor.testkit.sim.*
Move reusable scenario YAML resources into sor-testkit resources.
Update sor-test-server to depend on sor-testkit and import the moved classes.
Keep server launchers and HTTP endpoint classes in sor-test-server.
```

Out of scope:

```text
Do not move NotebookScenarioHttpServer into sor-testkit.
Do not move SimulatorServerApplication or SimulatorServerMain into sor-testkit.
Do not introduce scenario concepts into sor-api or sor-core.
Do not change scenario behavior except where imports/resource paths require it.
```

Acceptance criteria:

```text
P9-TESTKIT-MOVE-001 Given :sor-testkit:test, then scenario loader, runner,
                    simulator adapters, and YAML resource tests pass in the
                    new module.
P9-TESTKIT-MOVE-002 Given :sor-test-server:test, then notebook/demo HTTP
                    endpoints still run scenarios through sor-testkit and the
                    real SorEngine path.
P9-TESTKIT-MOVE-003 Given source scans, then no reusable scenario/simulator
                    implementation remains in sor-test-server except server
                    launch/wiring classes and HTTP DTOs.
P9-TESTKIT-MOVE-004 Given architecture tests, then sor-core has no dependency
                    on sor-testkit and no scenario concept leaks into engine
                    packages.
P9-TESTKIT-MOVE-005 Given docs and run scripts, then test selectors and
                    module descriptions use the new sor-testkit location.
```

### P9-TC-017 — Production JDK Refresh And Compact Object Headers With ZGC

Upgrade the project runtime baseline to the latest production-grade JDK 25
patch release available to the build environment and enable Compact Object
Headers with ZGC after verification. Phase 8 treated this as deferred, but JEP
519 delivered Compact Object Headers as a product feature in JDK 25, and the
current runtime accepts `-XX:+UseZGC -XX:+UseCompactObjectHeaders` together.

Required behavior:

```text
JAVA_HOME points to a production JDK 25 patch release.
Runtime scripts and container defaults use ZGC plus Compact Object Headers.
Build and benchmark documentation records the exact JDK version and VM flags.
The feature is guarded by regression and JMH evidence before being declared the
default production runtime profile.
```

The expected production VM profile is:

```text
-XX:+UseZGC
-XX:+UseCompactObjectHeaders
-XX:+AlwaysPreTouch
```

Acceptance criteria:

```text
P9-JDK-001 Given the configured JAVA_HOME, when `java -version` runs, then it
           reports a production OpenJDK 25 patch release or newer compatible
           production JDK explicitly approved by the spec.
P9-JDK-002 Given the configured JVM flags, when the VM starts, then
           `-XX:+UseZGC -XX:+UseCompactObjectHeaders` are accepted together.
P9-JDK-003 Given `-XX:+PrintFlagsFinal`, when the runtime profile is inspected,
           then `UseZGC=true` and `UseCompactObjectHeaders=true` are present.
P9-JDK-004 Given the Compact Object Headers profile, when the full Java test
           suite runs, then it passes without new failures.
P9-JDK-005 Given the Compact Object Headers profile, when hot-path JMH gates run,
           then route-decision latency and allocation gates remain within the
           documented thresholds.
P9-JDK-006 Given packaging scripts, container metadata, Helm values, and docs,
           when inspected, then their production runtime profile uses the same
           documented JDK and VM flags or explicitly explains why a local/dev
           profile differs.
P9-JDK-007 Given the old Phase 8 deferred note, when Phase 9 documentation is
           updated, then Compact Object Headers with ZGC is no longer listed as
           deferred after Phase 9.
```

### P9-TC-018 — Startup Hydration API And Snapshot Contracts

Define the public/control-plane contracts for startup hydration. Earlier Phase
9 cards give the engine local market and order books; this card defines the
typed inputs, outputs, validation fields, and diagnostics needed to hydrate
those books from OMS/EMS and market-data snapshots before accepting new parent
intent.

Required behavior:

```text
OMS/EMS provides active parent/child order snapshots through a control-plane
snapshot SPI or startup seed.
Order snapshots carry parent records and child records. Child records must
include childOrderId, parentOrderId, instrumentId where needed, venueId, side,
price/limit where applicable, original quantity, leaves quantity, filled
quantity, status, and last update metadata.
Market-data source provides an initial venue-aware market snapshot or replayable
snapshot stream before live increments are considered sufficient.
Hydration request/summary DTOs expose scope, source timestamps, sequence,
checksums, counts, replay-safety, accepted/rejected state, and failure reasons.
Snapshots carry snapshotId plus as-of sequence/timestamp metadata. Applying the
same accepted snapshotId again is idempotent and must not double-create parent
or child state.
The API is control-plane only and must not be part of the hot-path
ParentOrderRequest flow.
```

Recommended public/control-plane shape:

```java
public interface SorStartupStateSource {
    OrderStateSnapshot initialOrderState();
    MarketDataSeedSnapshot initialMarketData();
}

public record OrderStateSnapshot(
    String snapshotId,
    long asOfSequence,
    long asOfEpochNanos,
    ParentOrderStateSnapshot[] parents,
    ChildOrderStateSnapshot[] children
) {}

public interface SorControlPlane {
    SorStartupHydrationSummary hydrate(SorStartupHydrationRequest request);
    SorStateSummary stateSummary();
    MarketDataSnapshotSummary marketDataSnapshot();
}
```

Names may differ if the implementation finds a cleaner API, but the boundary
must remain explicit: startup seeding is control-plane lifecycle input, not
hot-path order submission.

Required tests:

```text
StartupHydrationApiTest
  validates request/summary DTOs, null handling, defensive copies, and failure
  reason fields.

StartupSnapshotContractTest
  validates parent/child/market snapshot dimensions, IDs, quantities,
  timestamps, sequence/checksum metadata, and empty-snapshot declarations.

OrderStateSnapshotChildContractTest
  validates that child snapshots carry venue, quantity, status, price, and
  parent linkage fields needed to rebuild venue-resting child state.
```

Acceptance criteria:

```text
P9-HYDRATE-API-001 Given startup hydration DTOs, when constructed with valid
                   order and market snapshots, then they preserve scope,
                   counts, timestamps, checksums, and replay-safety metadata.
P9-HYDRATE-API-002 Given invalid hydration DTO inputs, when constructed, then
                   they fail with clear validation messages.
P9-HYDRATE-API-003 Given no active OMS/EMS orders, when startup is represented,
                   then the API supports an auditable empty order-state
                   snapshot with source sequence and timestamp.
P9-HYDRATE-API-004 Given public API review, then hydration APIs live in
                   sor-api/control-plane surfaces and are not exposed through
                   hot-path ParentOrderRequest.
P9-HYDRATE-API-005 Given an order-state snapshot, then it includes child-order
                   records sufficient to rebuild venue-resting children,
                   pending child quantity, and future self-liquidity guards.
P9-HYDRATE-API-006 Given the same accepted snapshotId is applied twice, then
                   the second apply is idempotent and does not duplicate parent
                   or child state.
P9-HYDRATE-API-007 Given one snapshot contains duplicate parentOrderId or
                   childOrderId values, then validation treats that as corrupt
                   input, not as idempotent replay.
```

### P9-TC-019 — Engine Startup Hydration And Readiness Gate

Implement the engine behavior that copies startup snapshots into engine-owned
state and keeps the engine not ready until hydration commits successfully.

Required behavior:

```text
SOR starts in CONSTRUCTED, progresses through explicit readiness phases, and
does not rely on a single boolean for operational state.
Recommended phases: CONSTRUCTED, WARMED, MARKET_HYDRATING, ORDER_RECONCILING,
READY, FAILED, RECOVERY_REQUIRED.
The engine copies startup market snapshots into MarketBookState.
The engine atomically copies OMS/EMS active parent/child snapshots into
EngineOrderBook. It must rebuild parents and children together, then derive
parent pendingChildQuantity from child state rather than trusting a parent-only
seed.
Snapshot apply is a warm-path bulk operation. It must not reuse hot-path
emission methods such as recordChildOrder(...), and it may allocate temporary
validation/build structures before atomically publishing rebuilt state.
Children are sorted by stable ID before folding into parent aggregates so
derivation, checksums, and scenario replay evidence are input-order independent.
If parent-level snapshot fields carry aggregate values such as leaves,
filled quantity, or pending child quantity, the engine must cross-check those
values against child-derived aggregates. Disagreement beyond documented
tolerance is a fail-closed reconciliation error, not a silent overwrite.
The engine validates dimensions, IDs, quantities, timestamps, and sequence or
freshness metadata before committing state.
The engine rejects new ParentOrderRequest while hydration is missing, failed, or
not yet committed. The rejection reason must be phase-specific and hot-path
safe: no per-call dynamic String construction or blocking work.
The engine becomes READY only after order-state hydration, market-state
hydration, policy availability, and startup validation all succeed.
Hydration result, checksums, counts, and failure reasons are exposed through
diagnostics and lifecycle/audit evidence.
Market hydration is defined by configured freshness/coverage requirements, such
as top-of-book availability within the staleness window for each required
instrument/venue. Missing feeds must time out into FAILED or RECOVERY_REQUIRED
rather than hang startup forever.
During hydration, live quotes, fills, rejects, cancels, and venue reports must
be buffered or sequenced against the snapshot as-of watermark, then drained,
dropped, or replayed deterministically after snapshot commit. The engine must
not lose or double-apply live deltas that race with startup snapshots.
The live-delta buffer must be bounded and preferably preallocated. Buffer
overflow during hydration is a FAILED/RECOVERY_REQUIRED condition because the
engine can no longer prove no delta was lost. Deltas at or before the snapshot
as-of sequence are dropped as already represented; deltas after the watermark
are applied once in sequence order.
OMS/EMS order snapshots are authoritative for working order/venue state; SOR
persistence remains authoritative for SOR-owned policy, lineage, lifecycle, and
audit evidence. Startup recovery must define precedence so parent/child state is
not double-created from multiple sources.
Reconciliation conflicts fail closed. Unknown venues/instruments, duplicate
IDs, impossible quantities, missing parent linkage, indeterminate venue state,
or stale source metadata must move the engine to FAILED or RECOVERY_REQUIRED
with audited loaded/quarantined/rejected counts.
Readiness phases are forward-only except explicit lifecycle operations. Normal
startup may progress toward READY, READY may move to RECOVERY_REQUIRED or FAILED
on unrecoverable state loss, and a PURGE_AND_REPOPULATE reset may intentionally
re-enter MARKET_HYDRATING before rebuilding state.
```

Required tests:

```text
SorEngineStartupHydrationTest
  proves the engine is not ready before hydration, rejects submitParentOrder
  while hydrating, accepts after successful hydration, and records diagnostics.

OrderStateHydrationTest
  proves OMS/EMS parent and child snapshots populate EngineOrderBook without
  calling submitParentOrder as a fake recovery path.

MarketStateHydrationTest
  proves startup market snapshots populate MarketBookState with venue-aware
  cells, sequence/checksum, and timestamp/freshness evidence.

StartupHydrationFailureTest
  proves invalid dimensions, crossed/negative market data, impossible order
  quantities, duplicate IDs, and stale snapshots fail closed with audit evidence.

ReadinessPhaseGateTest
  proves readiness phases are exposed through stateSummary()/ready endpoints and
  submitParentOrder rejects with phase-specific, hot-path-safe reason codes.

LiveDeltaDuringHydrationTest
  proves live quotes and venue execution reports that arrive during hydration
  are buffered or watermarked, then applied exactly once in deterministic order.

RecoveryPrecedenceAndQuarantineTest
  proves OMS/EMS working order state and SOR persistence state do not double-load
  parents/children, and invalid snapshot records are quarantined or fail closed.

BulkSnapshotApplyInvariantTest
  proves snapshot recovery uses a warm-path bulk apply, sorts child records
  before folding, derives parent aggregates from children, and cross-checks any
  parent aggregate fields supplied by the snapshot.

SnapshotIdempotencyAndDuplicateIdTest
  proves reapplying the same accepted snapshotId is a no-op while duplicate IDs
  inside a single snapshot fail closed.

BoundedDeltaBufferOverflowTest
  proves hydration fails closed when the live-delta buffer overflows before the
  snapshot can be committed.

ReadinessPhaseTransitionTest
  proves startup phases move forward only, READY can degrade to
  RECOVERY_REQUIRED/FAILED, and PURGE_AND_REPOPULATE intentionally re-enters
  MARKET_HYDRATING.
```

Acceptance criteria:

```text
P9-HYDRATE-ENGINE-001 Given a newly built engine, when startup hydration has not
                      completed, then isReady() is false and submitParentOrder
                      fails with a clear not-ready/hydration message.
P9-HYDRATE-ENGINE-002 Given a valid OMS/EMS order-state snapshot, when
                      hydration runs, then active parent/child state is loaded
                      into EngineOrderBook without replaying submitParentOrder.
P9-HYDRATE-ENGINE-003 Given a valid venue-aware market-data snapshot, when
                      hydration runs, then MarketBookState contains the
                      expected instrument/venue cells and exposes
                      sequence/checksum/freshness diagnostics.
P9-HYDRATE-ENGINE-004 Given order-state, market-state, policy, and validation
                      all succeed, when hydration commits, then the engine
                      becomes ready and accepts new parent intent.
P9-HYDRATE-ENGINE-005 Given invalid or stale startup data, when hydration runs,
                      then the engine remains not ready, rejects new parent
                      intent, and emits auditable failure evidence.
P9-HYDRATE-ENGINE-006 Given diagnostics after hydration, then state summaries
                      expose counts, checksums, timestamps, last hydration
                      status, and failure reasons without returning mutable
                      engine internals.
P9-HYDRATE-ENGINE-007 Given a parent/child order-state snapshot, when hydration
                      commits, then children are rebuilt by childOrderId,
                      parent aggregates are derived consistently, and
                      pendingChildQuantity matches venue-resting leaves.
P9-HYDRATE-ENGINE-008 Given a parent order is submitted during MARKET_HYDRATING
                      or ORDER_RECONCILING, then the engine rejects with a
                      phase-specific hot-path-safe reason.
P9-HYDRATE-ENGINE-009 Given live quotes/fills/rejects race with snapshot apply,
                      then snapshot as-of metadata or buffering prevents lost
                      and double-applied deltas.
P9-HYDRATE-ENGINE-010 Given snapshot reconciliation conflicts, then startup
                      fails closed into FAILED or RECOVERY_REQUIRED with
                      audited loaded, quarantined, and rejected counts.
P9-HYDRATE-ENGINE-011 Given parent aggregate fields disagree with child-derived
                      aggregate state, then hydration fails closed instead of
                      silently overwriting one side.
P9-HYDRATE-ENGINE-012 Given a snapshot's children arrive in different orders,
                      then bulk apply sorts before folding and produces the
                      same parent aggregates and checksum.
P9-HYDRATE-ENGINE-013 Given the same accepted snapshotId is reapplied, then it
                      is a no-op; given duplicate IDs inside one snapshot, then
                      hydration fails closed.
P9-HYDRATE-ENGINE-014 Given the hydration live-delta buffer overflows, then the
                      engine moves to FAILED or RECOVERY_REQUIRED and refuses
                      READY.
P9-HYDRATE-ENGINE-015 Given readiness phase transitions, then they are
                      forward-only except documented degradation or explicit
                      reset/repopulate lifecycle operations.
```

### P9-TC-020 — Scenario And Testkit Hydration Path

Update scenario reset-and-repopulate so testkit uses the same startup
hydration/control-plane path as production startup. Scenario code may still
generate artificial data, but it must feed that data through the public
hydration and live engine APIs rather than mutating engine internals or faking
recovered orders with `submitParentOrder`.

Required behavior:

```text
Scenario reset purges selected runtime state through SorControlPlane.
Scenario repopulation hydrates market and order snapshots through the same API
used by production startup.
Scenario order-state snapshots include child records and are applied
atomically, even when a scenario starts with venue-resting child liquidity.
Scenario parent orders after hydration go through SorEngine.submitParentOrder.
Scenario evidence records hydration summary, reset summary, market checksum,
order counts, route events, child events, fills, rejects, and final state.
Hydration order is deterministic: records are sorted by stable IDs or generated
from a documented seed before application, so replay checksums and divergence
tests stay stable.
Scenario hydration must exercise the same bulk snapshot apply as production
startup and must not route recovered child orders through hot-path child
emission methods.
Hydration emits lifecycle/audit events so replay evidence can reconstruct the
same starting state.
sor-core remains scenario-agnostic and must not import sor-testkit/test-server
packages.
```

Required tests:

```text

ScenarioHydrationPathTest
  proves scenario purge-and-repopulate uses the same public/control-plane
  hydration path and then submits parent orders through SorEngine.

ScenarioDeterministicHydrationTest
  proves repeated runs with the same seed apply hydration records in the same
  order and produce stable starting-state checksums and lifecycle evidence.

ScenarioSnapshotInvariantTest
  proves shuffled scenario child records produce the same hydrated aggregates
  and checksum, and parent/child aggregate disagreement fails closed.

ScenarioHydrationNegativeGateTest
  proves a parent submitted during MARKET_HYDRATING is rejected with the
  expected phase-specific reason.

ArchitectureStartupHydrationTest
  proves sor-core does not import sor-testkit/test-server scenario classes and
  that production startup hydration APIs live in sor-api/control-plane surfaces.
```

Acceptance criteria:

```text
P9-HYDRATE-SCEN-001 Given scenario reset-and-repopulate, when testkit
                    repopulates state, then it uses the same
                    hydration/control-plane path as production startup and
                    records replay-safe evidence.
P9-HYDRATE-SCEN-002 Given scenario parent orders after hydration, when replayed,
                    then they are submitted through SorEngine and not used to
                    fake recovered startup state.
P9-HYDRATE-SCEN-003 Given scenario evidence, when a run completes, then reset,
                    hydration, market checksum, order counts, route/child/fill/
                    reject events, and final state are linked.
P9-HYDRATE-SCEN-004 Given architecture checks, then scenario/testkit concepts do
                    not leak into sor-core or hot-path API classes.
P9-HYDRATE-SCEN-005 Given full module checks, then API, core, testkit,
                    transport, and test-server tests prove hydration behavior
                    without introducing scenario concepts into sor-core.
P9-HYDRATE-SCEN-006 Given a scenario starts with active venue-resting children,
                    then testkit hydrates child snapshots through the same API
                    and the resulting parent aggregates are deterministic.
P9-HYDRATE-SCEN-007 Given the same scenario seed and snapshot inputs, when
                    replayed twice, then hydration lifecycle evidence and
                    starting-state checksums are stable.
P9-HYDRATE-SCEN-008 Given scenario child snapshots are supplied in different
                    orders, then sorted bulk apply yields the same aggregates
                    and replay checksum.
P9-HYDRATE-SCEN-009 Given a corrupt scenario snapshot with parent/child
                    aggregate mismatch or duplicate child IDs, then scenario
                    hydration fails closed with replay evidence.
```

---

## 9.5 OMS/EMS Input Contract

The SOR receives parent-order intent and recovery state from OMS/EMS or the
execution algo layer.

For new orders, `ParentOrderRequest` is the hot-path entry point. Phase 9 may
extend it or attach a richer context object for:

```text
external parent/client order ID
instrument
side
quantity and remaining quantity
limit price
order type
time in force
urgency
strategy parameters
venue allow/deny lists
client/account/risk profile
regulatory/compliance flags
```

For recovery, scenario seeding, or reattach, Phase 9 must not fake prior state
by replaying `submitParentOrder` calls unless explicitly running a historical
event replay. It should define a control-plane snapshot:

```text
ParentOrderStateSnapshot
ChildOrderStateSnapshot
OrderStateSeed
SorStateSeed
```

This seed is control-plane input. It is not part of the per-order hot path.

For production startup, the seed is mandatory when OMS/EMS has live working
orders for the engine's scope. If OMS/EMS declares there are no active orders,
the declaration itself should be represented as an auditable empty snapshot with
source timestamp, scope, and sequence metadata.

---

## 9.6 Market Data Contract

The SOR's market book is a local routing view, not the market-data plant's
golden record. It must be keyed by:

```text
instrumentId + venueId
```

Minimum maintained state:

```text
best bid price
best ask price
best bid quantity
best ask quantity
last update timestamp
sequence/checksum
stale/invalid/quarantined status where configured
```

Additional state may include L2/L3 depth, auction state, feed health, session
state, fee/rebate metadata, and derived venue quality signals. These may remain
in existing `sor-core.state` classes but must be fed through clear ownership
boundaries.

---

## 9.7 Migration Notes

1. Add `venueId` to `Quote` and update simulator publishers.
2. Add `MarketBookState.clear()` or equivalent reset behavior.
3. Add engine-owned `MarketBookState` to `SorEngineImpl`.
4. Wire `onQuote()` to update `MarketBookState`.
5. Add internal order-state structures or connect existing ones into
   `SorEngineImpl`.
6. Introduce `SorControlPlane` in `sor-api` or a dedicated control module.
7. Move `sor-test-server` reset endpoints onto `SorControlPlane`.
8. Convert main scenario replay to real-path mode.
9. Keep direct simulator `currentBook()` tests as simulator-only tests.
10. Add architecture tests preventing simulator-local state from being used as
    primary SOR evidence.
11. Promote `sor-transport-http-control` as the supported built-in user HTTP
    module over `SorEngine`/`SorControlPlane`.
12. Keep `/scenario/*` and notebook demo endpoints in `sor-test-server` unless
    a future optional scenario module is created.
13. Update Python notebook helpers and docs to distinguish user HTTP endpoints
    from test-server scenario endpoints.
14. Add endpoint, OpenAPI, architecture, and example coverage for the built-in
    user HTTP module.
15. Introduce `sor-testkit` for reusable scenario/simulator support; keep
    `sor-api` and `sor-core` scenario-agnostic.
16. Move scenario replay, simulator adapters, and reusable YAML scenario
    resources into `sor-testkit`; keep `sor-test-server` as a thin runnable
    HTTP/demo wrapper.
17. Refresh the production JDK baseline and enable Compact Object Headers with
    ZGC after test and benchmark evidence.
18. Add startup hydration API and snapshot contracts for OMS/EMS order-state
    snapshots and market-data snapshots.
19. Implement engine startup hydration and readiness gating so the engine
    rejects new parent intent until hydration commits successfully.
20. Update scenario/testkit reset-and-repopulate to use the same hydration path
    as production startup.

---

## 9.8 Exit Criteria

Phase 9 is complete only when all of the following are true:

```text
SorEngineImpl maintains a venue-aware engine-owned MarketBookState.
SorEngineImpl maintains engine-owned active parent/child order state.
Scenario reset can purge and repopulate engine-owned state through control APIs.
The main scenario runner routes through SorEngine rather than simulator-local
book shortcuts.
Venue adapters receive child orders from engine ring writers and return
fills/rejects through callbacks.
Diagnostics expose immutable state summaries/checksums.
Reset and replay are audited.
Existing simulator-local tests remain useful but are not the main SOR proof.
API/SPI/ABI compatibility tests document all public changes.
Built-in user HTTP endpoints are available from an optional module outside
sor-core and outside sor-test-server.
Scenario/demo endpoints remain clearly separated from supported user HTTP
control endpoints.
Reusable scenario/simulator support lives in sor-testkit, not in sor-core and
not directly inside the runnable test-server wrapper.
SorEngine and sor-api remain scenario-agnostic.
The production runtime profile uses a current production JDK 25 patch release
with ZGC and Compact Object Headers enabled, backed by regression and JMH
evidence.
Startup hydration from OMS/EMS order-state snapshots and market-data snapshots
is explicit, audited, and required before accepting new parent intent.
The full Gradle and Python client test suites pass.
```

---

## 9.9 Deferred After Phase 9

The following remain deferred until Phase 9 exit criteria are met:

```text
C++ L0 route-decision port
multi-tenant hosting
gRPC transport
real exchange adapters
```

The reason is deliberate: accelerating or multiplying an engine whose state
ownership is ambiguous would only make the ambiguity harder to remove later.
