# CI Test Profiles

This project keeps Java, C++, CUDA-aware native artifacts, documentation guards,
integration tests, and benchmark smoke tests behind repeatable local scripts.

## Full Check

```bash
scripts/run_tests.sh all
```

Runs Gradle `check`, including JUnit and native CTest. The checked-in
`./gradlew` is a lightweight launcher that delegates to a developer-installed
Gradle binary rather than a bundled wrapper JAR.

## Unit Profile

```bash
scripts/run_tests.sh unit
```

Runs focused unit-test packages for model, state, policy, and optimizer code.

## Integration Profile

```bash
scripts/run_tests.sh integration
```

Runs integration, E2E, and API workflow tests.

## Simulator Deterministic Profile

```bash
scripts/run_tests.sh simulator
```

Runs deterministic simulator contract coverage only. This profile is for
low-level simulator unit tests such as `SimulatorTest`,
`MarketDataSimulatorTest`, `VenueBehaviorSimulatorTest`, and session/throttle
simulator tests.

This profile should not run scenario replay tests. Keeping it narrow makes
single-simulator regressions easy to diagnose.

## Scenario-Driven Simulation Profile

```bash
scripts/run_tests.sh scenario
```

Runs replayable scenario simulation coverage. This profile is for tests backed
by scenario orchestration classes such as `ScenarioSpec`, `ScenarioClock`,
`ScenarioRandoms`, `ScenarioRunner`, and `ScenarioSummary`.

Expected coverage:

```text
fixed-seed scenario replay
multi-tick market-state evolution
regime-conditioned spread/volatility/depth
venue profiles
stale/outage/zero-liquidity windows
scenario-to-feature integration
scenario optimizer-lineage integration
scenario end-to-end replay summaries
```

The scenario profile should include:

```text
com.nitroj.adaptive.quantum.sor.scenario.*
com.nitroj.adaptive.quantum.sor.e2e.SorEndToEndTest.replayableScenarioProducesEquivalentSummary
com.nitroj.adaptive.quantum.sor.e2e.SorEndToEndTest.scenarioLiquidityDisappearanceRoutesSafely
```

The deterministic simulator profile must stay focused on
`com.nitroj.adaptive.quantum.sor.sim.*` tests and must not include the scenario
package. This separation keeps direct simulator regressions distinct from
scenario orchestration failures.

The simulator profile must not include the scenario package.

## Live Jupyter Scenario Evidence

Live Jupyter scenario testing is interactive evidence, not a replacement for
the Gradle scenario profile. When implemented, notebooks should exercise:

```text
POST /scenario/reset
POST /scenario/run
GET /scenario/summary
GET /scenario/events
```

The notebook flow must show the selected reset mode, the reset summary, and the
scenario summary. Purging live engine state must be explicit and auditable.

## Policy Compile Profile

```bash
scripts/run_tests.sh policy
```

Runs policy lint, compile, and validation tests.

## Native Profile

```bash
scripts/run_tests.sh native
```

Runs the CMake/CTest native tests.

## Benchmark Manual Profile

```bash
scripts/run_benchmarks.sh manual
scripts/run_benchmarks.sh jmh
scripts/run_benchmarks.sh comparison
```

The manual profile runs the benchmark harness smoke test. The JMH profile runs
`PolicyDrivenSorJmhBenchmark.strictRouteInto` with the GC profiler to verify the
strict caller-owned L0 route path allocation rate. The comparison profile runs
adaptive/static comparison reporting tests, runs static SOR and adaptive SOR
selection over the same realistic feature dataset, compares actual selected-row
labels, and writes:

```text
build/reports/benchmarks/sor-comparison-report.md
```

Override inputs and output paths:

```bash
ADAPTIVE_QUANTUM_SOR_COMPARISON_DATASET=python/examples/sor_notebook_features_100k.csv \
ADAPTIVE_QUANTUM_SOR_COMPARISON_REPORT=build/reports/benchmarks/custom-report.md \
scripts/run_benchmarks.sh comparison
```

These profiles are repeatable evidence for `P1-BENCH-001` and `P1-BENCH-003`.
