# Adaptive Quantum SOR

Adaptive Quantum SOR is a Java-first implementation of a policy-driven smart
order router. It models the full local loop around simulated venues, replayable
market scenarios, feature/stat state, ML signal ingestion, strategic and
tactical policy optimization, policy publication, execution, audit, metrics,
and notebook-driven research workflows.

The project follows `adaptive_quantum_sor_spec_v1.md` plus the Phase 8
productization spec in `adaptive_quantum_sor_spec_phase8.md`. Phase completion
reports in `docs/` record the implemented acceptance criteria and known limits.

## Current Runtime Shape

Phase 8 reshapes Adaptive Quantum SOR from one root Java application into a
multi-project framework with a small public API, an embedded engine, explicit
transports, simulator-owned integrations, and a standalone sample server.

```text
sor-api                    Public engine contract, DTOs, events, and SPI.
sor-core                   Embedded engine plus transitional compatibility code.
sor-test-server             SPI-facing simulators, scenarios, and sample server.
sor-transport-aeron        Low-latency remote transport over SBE/Aeron.
sor-transport-http-control Research and ops HTTP control plane.
sor-optimizers-native      Native optimizer boundary.
sor-client-java            Java remote client surface.
sor-client-python          Python/Jupyter client package.
```

The main deployment shapes are:

```text
Embedded        Integrator JVM links sor-core and calls the SorEngine API.
Out-of-process Integrator uses Aeron/SBE against the sor-test-server sample server.
Research/Ops   HTTP control plane for notebooks, health, metrics, and OpenAPI.
```

The launchable simulator sample server application class is:

```text
com.nitroj.sor.sim.server.SimulatorServerApplication
```

The server exposes the HTTP control-plane API for Python and Jupyter:

```bash
./gradlew :sor-test-server:run --args='--http-control-port=8080'
```

The same server can also be launched through `scripts/run_engine.sh` from a
source checkout, through `sor-test-server/run_sample_server.sh` from a packaged
`lib/*` distribution, or through the Helm chart in
`sor-test-server/helm/adaptive-quantum-sor`.

## Repository Map

```text
sor-api/                     Public API, DTOs, events, SPI, and ABI tests.
sor-core/                    Engine internals, policy, execution, and native build.
sor-test-server/              Simulator adapters, scenarios, sample server, Jib image, Helm chart.
sor-transport-aeron/         Aeron transport client/server wrapper.
sor-transport-http-control/  HTTP health, readiness, metrics, OpenAPI, and order control.
sor-observability/           Metrics and observability helpers.
sor-optimizers-native/       Native optimizer boundary module.
sor-client-java/             Java SDK and quickstart.
sor-client-python/           Python client package.
cpp/                         CMake native tactical and strategic optimizer artifacts.
notebooks/                   JupyterLab demo and research notebooks.
tools/notebook-helpers/      Notebook-only widgets, reports, and API helper code.
tools/python-research/       Research datasets, scenario catalog, and model scripts.
sor-test-server/src/main/resources/scenarios/
                             User-readable replayable scenario catalog.
docs/                        Categorized architecture, integration, release, testing, and report docs.
scripts/                     Local test, benchmark, engine, and Jupyter launch helpers.
```

## Prerequisites

- Java 25. The scripts default `JAVA_HOME` to
  `/usr/lib/jvm/java-25-openjdk-amd64` for Phase 8 paths where applicable.
- Gradle available to the lightweight `./gradlew` launcher. The repository does
  not currently bundle a full Gradle wrapper JAR; `./gradlew` delegates to the
  Gradle installation in the developer environment.
- CMake and a C++ toolchain for native build and CTest validation
- Python 3 with `venv` support for notebook workflows

The Jupyter launcher creates `.venv-notebook` and installs notebook dependencies
there automatically:

```bash
scripts/start-jupyter-lab.sh
```

## Build And Test

Run the full local validation profile:

```bash
scripts/run_tests.sh all
```

This delegates to Gradle `check`, which runs JUnit tests and native CTest
coverage through the CMake-backed native build.

Focused profiles are available when you want a smaller loop:

```bash
scripts/run_tests.sh unit
scripts/run_tests.sh integration
scripts/run_tests.sh simulator
scripts/run_tests.sh scenario
scripts/run_tests.sh policy
scripts/run_tests.sh native
```

See `docs/testing/CI_TEST_PROFILES.md` for the exact test coverage behind each profile.

## Run The Java Adaptive Quantum SOR

Start the simulator sample server from source:

```bash
./gradlew :sor-test-server:run
```

The root launcher wraps the same Gradle target and adds the standard JVM flags:

```bash
scripts/run_engine.sh --http-control-port=8080
```

Start it with an explicit YAML file:

```bash
./gradlew :sor-test-server:run --args='--config=/path/to/adaptive-quantum-sor.yaml'
```

Default configuration lives at:

```text
sor-core/src/main/resources/adaptive-quantum-sor.yaml
```

The configuration supports dense Adaptive Quantum SOR dimensions, runtime mode, and strict
unknown-key startup validation.

For a packaged distribution where jars are already laid out under `lib/*`, use:

```bash
cd sor-test-server
./run_sample_server.sh --http-control-port=8080
```

For Kubernetes deployment, use the Helm chart:

```bash
helm lint sor-test-server/helm/adaptive-quantum-sor/
helm install aqs sor-test-server/helm/adaptive-quantum-sor/ --dry-run
helm install aqs sor-test-server/helm/adaptive-quantum-sor/
```

## Jupyter Demo Workflow

Start the engine API and JupyterLab workspace:

```bash
scripts/start-jupyter-lab.sh
```

The launcher builds Java classes, starts `NotebookScenarioApiLauncher` with the
legacy notebook API on `http://127.0.0.1:8080`, sets `PYTHONPATH=tools/notebook-helpers:tools/python-research`, and
opens the notebook workspace. Python notebooks send order, scenario, policy,
and stats requests to that engine over HTTP.

Useful notebooks:

```text
notebooks/submit_parent_order.ipynb             widget-driven parent order submission report
notebooks/live_stats_monitor.ipynb              widget-driven stats, policy, and order summary monitor
notebooks/scenario_runner.ipynb                 widget-driven scenario execution report
```

The notebooks use `ipywidgets` controls for normal demo operation, plus
professional report sections for run manifests, result lists, KPI cards, fill
breakdowns, and lifecycle/evidence tables.

Optional ports:

```bash
ADAPTIVE_QUANTUM_SOR_API_PORT=8081 ADAPTIVE_QUANTUM_SOR_JUPYTER_PORT=8890 scripts/start-jupyter-lab.sh
```

See `notebooks/README.md` for the interactive workflow.

## Scenarios

The scenario catalog under `sor-test-server/src/main/resources/scenarios/<category>/*.yaml` drives replayable
simulation tests and notebook/API scenario exploration. Scenarios cover baseline
replay, regime transitions, liquidity disappearance, stale feeds, venue outages,
toxic venues, lineage, live reset modes, feature/ML generation, risk, throttles,
capacity, multi-instrument behavior, and Phase 6 cross-parent batch allocation
cases.

Run deterministic scenario coverage:

```bash
scripts/run_tests.sh scenario
```

Explore the catalog from Python:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog list
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --tag liquidity
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog suggest zero-liquidity-safe-route
```

See `sor-test-server/src/main/resources/scenarios/README.md` for category and catalog details.

## Python Research And Notebook Helpers

The production Python client lives in `sor-client-python/`. Repository-local
notebook widgets and report templates live in
`tools/notebook-helpers/adaptive_quantum_sor_notebooks`, while offline research
datasets, schema helpers, scenario catalog commands, training, and comparisons
live in `tools/python-research`.

Common setup:

```bash
export PYTHONPATH="$PWD/tools/notebook-helpers:$PWD/tools/python-research${PYTHONPATH:+:$PYTHONPATH}"
```

Train local research models:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 tools/python-research/scripts/train_models.py \
  --dataset tools/python-research/examples/sor_notebook_features_100k.csv \
  --output-dir build/ml/artifacts
```

Run static-vs-adaptive comparison reporting:

```bash
scripts/run_benchmarks.sh comparison
```

Run the strict hot-path JMH allocation benchmark:

```bash
scripts/run_benchmarks.sh jmh
```

See `tools/python-research/README.md` for datasets, artifact contracts, and helper APIs.

## Native Optimizers

Gradle owns the native build:

```bash
./gradlew :sor-core:nativeBuild
./gradlew :sor-core:nativeTest
```

Native artifacts live in `cpp/` and are configured with CMake. Java tests pass
the native build output directory through `sor.native.lib.dir`.

## Documentation

Start with:

```text
adaptive_quantum_sor_spec_v1.md
adaptive_quantum_sor_spec_phase8.md
docs/README.md
docs/architecture/ARCHITECTURE.md
docs/testing/CI_TEST_PROFILES.md
docs/architecture/SEQUENCE_DIAGRAMS.md
docs/integration/INTEGRATING_AS_EMBEDDED.md
docs/integration/INTEGRATING_OVER_AERON.md
docs/reports/phase-1-7/PHASE_1_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_2_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_3_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_4_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_5_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_6_COMPLETION_REPORT.md
docs/reports/phase-1-7/PHASE_7_COMPLETION_REPORT.md
docs/reports/phase-8/PHASE_8_COMPLETION_REPORT.md
```

The phase reports are evidence documents: they summarize implemented scope,
validation commands, planned follow-up, and known limits.
