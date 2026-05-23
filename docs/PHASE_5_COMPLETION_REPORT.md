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
- User-readable scenario files under `sor-test-server/src/main/resources/scenarios/<category>/*.yaml` load through
  `ScenarioDefinitionLoader` into `ScenarioSpec`.
- The checked-in scenario catalog contains at least 62 files covering baseline,
  regime, liquidity, stale feed, outage, toxic venue, lineage, failure, live
  reset, feature/ML, risk/throttle/capacity, multi-instrument, and
  zero-liquidity routing behavior.
- Optimizer input and dataset metadata can reference scenario id, seed, tick
  range, and summary checksum.
- E2E coverage proves replayable summaries and safe routing when liquidity
  disappears.
- P5-TC-008 live scenario runs now accept explicit parent order intents with
  `atTick`, `submitMode`, and optional `clientOrderRef`; `/scenario/run`
  returns per-parent-order route evidence and venue-level fill rows with
  instrument/venue names, quantities, prices, and notional.
- Scenario files can include optional `parentOrders` defaults for notebook/API
  exploration, and the Python scenario catalog surfaces those defaults as
  suggestions.

## Planned Follow-Up

P5-TC-008 is implemented for the live/Jupyter control-plane path. Remaining
future production work is limited to external order-management-system
integration, real exchange acknowledgements, and multi-user scheduling, which
are out of scope for this phase.

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
P5-SIM-017 live scenario parent order intents validate user fields
P5-SIM-018 live scenario parent orders route through the SOR execution path
P5-SIM-019 route results expose venue/instrument/price/fill evidence
P5-SIM-020 lifecycle evidence ties scenario id to user parent orders
P5-SIM-021 live scenario runs accept explicit parent order intents
X-E2E-001 local E2E scenario replay coverage
X-DOC-001 documentation guards for CI profiles and completion evidence
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
  explicit about reset mode, parent orders, and simulator-generated order opt-in.
- No numeric coverage report is configured; evidence is maintained through
  AC-to-test and class/method-to-test mappings.
