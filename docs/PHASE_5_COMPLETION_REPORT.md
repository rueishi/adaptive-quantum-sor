# Phase 5 Completion Report

Phase 5 adds the stateful stochastic/regime-aware simulator and separates
low-level deterministic simulator tests from scenario-driven replay tests.

## Implemented Scope

- `MarketDataSimulator` now keeps persistent instrument mids, venue quotes,
  displayed quantity, regime-aware shocks, venue profile spreads, and feed
  staleness hooks.
- Session, throttle, feed, parent-order, and venue-outcome simulators now expose
  scenario-aware APIs while preserving baseline deterministic helpers.
- `ScenarioRunner.run(ScenarioSpec)` is the canonical Gradle/JUnit scenario
  entry point and returns a stable `ScenarioSummary`.
- User-readable scenario files under `scenarios/<category>/*.yaml` load through
  `ScenarioDefinitionLoader` into `ScenarioSpec`.
- The checked-in scenario catalog contains at least 62 files covering baseline,
  regime, liquidity, stale feed, outage, toxic venue, lineage, failure, live
  reset, feature/ML, risk/throttle/capacity, multi-instrument, and
  zero-liquidity routing behavior.
- Optimizer input and dataset metadata can reference scenario id, seed, tick
  range, and summary checksum.
- E2E coverage proves replayable summaries and safe routing when liquidity
  disappears.

## Planned Follow-Up

P5-TC-008 is planned to connect live/Jupyter scenario runs to explicit
user-specified parent order intents. Today, scenario replay can generate
simulated parent orders internally, and `/orders` can accept live parent orders,
but those two flows are not yet unified into one "run this scenario with these
parent orders" API.

The P5-TC-008 target behavior is:

```text
ScenarioRunRequest.parentOrders[]
scenario YAML parentOrders suggestions/defaults
Python run_scenario(..., parent_orders=[...])
route/outcome/residual evidence per user parent order
notebook result tables for parent order route and venue outcome results
```

## Acceptance Criteria Evidence

Implemented ACs:

```text
P5-SIM-001 stateful deterministic scenario contract
P5-SIM-002 regime-aware L1 market data
P5-SIM-003 market-state-dependent venue outcomes
P5-SIM-004 regime-conditioned parent order/session behavior
P5-SIM-005 generated outcomes feed feature stats
P5-SIM-006 invalid generated state cannot escape
P5-SIM-007 feed/session outage state is explicit
P5-SIM-008 venue profiles affect quotes, fills, latency, and toxicity
P5-SIM-009 fixed seed replay is exact
P5-SIM-010 optimizer input lineage references scenario metadata
P5-SIM-011 dataset/export lineage references scenario metadata
P5-SIM-012 partial simulator failure blocks optimizer input publication
P5-SIM-013 deterministic and scenario CI profiles are separate
P5-SIM-014 tests avoid wall-clock timing
P5-SIM-015 scenario integration and E2E coverage are present
P5-SIM-016 docs and reproduction commands are present
X-E2E-001 local E2E scenario replay coverage
X-DOC-001 documentation guards for CI profiles and completion evidence
```

Planned ACs:

```text
P5-SIM-021 live scenario runs accept explicit parent order intents
```

Failed ACs:

```text
none
```

## Reproduction Commands

```bash
scripts/run_tests.sh simulator
scripts/run_tests.sh scenario
scripts/run_tests.sh integration
scripts/run_tests.sh all
```

The simulator profile is intentionally limited to deterministic low-level
simulator contract tests under `com.nitroj.adaptive.quantum.sor.sim`.

The scenario profile includes:

```text
com.nitroj.adaptive.quantum.sor.scenario.*
SorEndToEndTest.replayableScenarioProducesEquivalentSummary
SorEndToEndTest.scenarioLiquidityDisappearanceRoutesSafely
```

## Known Limits

- L2/L3 exchange queue priority and real exchange calendars remain out of scope.
- Live Jupyter reset/control-plane execution is documented for P5-TC-007 and is
  separate from isolated Gradle/JUnit scenario replay.
- No numeric coverage report is configured; evidence is maintained through
  AC-to-test and class/method-to-test mappings.
