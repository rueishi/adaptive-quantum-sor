# Adaptive Quantum SOR

Adaptive Quantum SOR is a Java-first implementation of a policy-driven smart
order router. It models the full local loop around simulated venues, replayable
market scenarios, feature/stat state, ML signal ingestion, strategic and
tactical policy optimization, policy publication, execution, audit, metrics,
and notebook-driven research workflows.

The project follows `adaptive_quantum_sor_spec_v1.md`. Phase completion reports
in `docs/` record the implemented acceptance criteria and known limits.

## Current Runtime Shape

The Adaptive Quantum SOR runs as a single Java process with explicit boundaries for control-plane
work, research workflows, and native optimizer integration.

```text
Jupyter/Python control plane
HTTP API and lifecycle stream
simulation, feature, and stats state
ML signal and optimizer input state
strategic optimizer
tactical optimizer
policy lint, compile, validate, publish
CPU SOR execution
audit, metrics, and reports
```

The launchable application class is:

```text
com.nitroj.adaptive.quantum.sor.AdaptiveQuantumSorApplication
```

The same application can expose the HTTP control-plane API for Python and
Jupyter:

```bash
./gradlew run --args='--api-port=8080'
```

## Repository Map

```text
src/main/java/      Java SOR runtime, simulators, policy, API, and optimizers
src/test/java/      JUnit scenario, integration, API, policy, ML, and E2E tests
cpp/                CMake native tactical and strategic optimizer artifacts
python/             Python helper package, datasets, model scripts, comparisons
notebooks/          JupyterLab demo and research notebooks
scenarios/          User-readable replayable scenario catalog
docs/               Architecture, sequence diagrams, CI profiles, phase reports
scripts/            Local test, benchmark, and Jupyter launch helpers
```

## Prerequisites

- Java 21
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

See `docs/CI_TEST_PROFILES.md` for the exact test coverage behind each profile.

## Run The Java Adaptive Quantum SOR

Start the Adaptive Quantum SOR with the default classpath configuration:

```bash
./gradlew run
```

Start it with an explicit YAML file:

```bash
./gradlew run --args=/path/to/adaptive-quantum-sor.yaml
```

Default configuration lives at:

```text
src/main/resources/adaptive-quantum-sor.yaml
```

The configuration supports dense Adaptive Quantum SOR dimensions, runtime mode, and strict
unknown-key startup validation.

## Jupyter Demo Workflow

Start the engine API and JupyterLab workspace:

```bash
scripts/start-jupyter-lab.sh
```

The launcher builds Java classes, starts `AdaptiveQuantumSorApplication` with the API on
`http://127.0.0.1:8080`, sets `PYTHONPATH=python`, and opens the notebook
workspace. Python notebooks send order, scenario, policy, and stats requests to
that engine over HTTP.

Useful notebooks:

```text
notebooks/submit_parent_order.ipynb             parent order submission panel
notebooks/live_stats_monitor.ipynb              stats and policy monitor
notebooks/scenario_runner.ipynb                 scenario reset and replay review
```

Optional ports:

```bash
ADAPTIVE_QUANTUM_SOR_API_PORT=8081 ADAPTIVE_QUANTUM_SOR_JUPYTER_PORT=8890 scripts/start-jupyter-lab.sh
```

See `notebooks/README.md` for the interactive workflow.

## Scenarios

The scenario catalog under `scenarios/<category>/*.yaml` drives replayable
simulation tests and notebook/API scenario exploration. Scenarios cover baseline
replay, regime transitions, liquidity disappearance, stale feeds, venue outages,
toxic venues, lineage, live reset modes, feature/ML generation, risk, throttles,
capacity, and multi-instrument behavior.

Run deterministic scenario coverage:

```bash
scripts/run_tests.sh scenario
```

Explore the catalog from Python:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog list
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog search --tag liquidity
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog suggest zero-liquidity-safe-route
```

See `scenarios/README.md` for category and catalog details.

## Python Research Helpers

The Python package in `python/adaptive_quantum_sor` supports notebooks, feature
dataset validation, scenario catalog access, API client calls, offline model
training, and static-vs-adaptive comparisons.

Common setup:

```bash
export PYTHONPATH="$PWD/python${PYTHONPATH:+:$PYTHONPATH}"
```

Train local research models:

```bash
PYTHONPATH=python python3 python/train_models.py \
  --dataset python/examples/sor_notebook_features_100k.csv \
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

See `python/README.md` for datasets, artifact contracts, and helper APIs.

## Native Optimizers

Gradle owns the native build:

```bash
./gradlew nativeBuild
./gradlew nativeTest
```

Native artifacts live in `cpp/` and are configured with CMake. Java tests pass
the native build output directory through `sor.native.lib.dir`.

## Documentation

Start with:

```text
adaptive_quantum_sor_spec_v1.md
docs/ARCHITECTURE.md
docs/CI_TEST_PROFILES.md
docs/SEQUENCE_DIAGRAMS.md
docs/PHASE_1_COMPLETION_REPORT.md
docs/PHASE_2_COMPLETION_REPORT.md
docs/PHASE_3_COMPLETION_REPORT.md
docs/PHASE_4_COMPLETION_REPORT.md
docs/PHASE_5_COMPLETION_REPORT.md
```

The phase reports are evidence documents: they summarize implemented scope,
validation commands, planned follow-up, and known limits.
