# Python Research Helpers

This directory contains the non-shipped Python research package, datasets, and
research scripts for Adaptive Quantum SOR. It supports offline feature analysis,
scenario catalog inspection, static-vs-adaptive comparison, and model-artifact
generation.

The Jupyter workflow guide now lives in `notebooks/README.md`. The notebooks
use pandas plus ipywidgets for friendly control panels and report-style output.

## What Is In This Folder

```text
tools/python-research/adaptive_quantum_sor_research/       local research helper package
tools/python-research/adaptive_quantum_sor_research/dataframe.py   dataset validation and artifact helpers
tools/python-research/adaptive_quantum_sor_research/scenario_catalog.py scenario catalog library
tools/python-research/adaptive_quantum_sor_research/schema.py      shared feature and prediction schemas
tools/python-research/examples/*.csv                      example research datasets
tools/python-research/models/*.py                         simple research model components
tools/python-research/scripts/generate_sor_dataset.py             deterministic dataset generator
tools/python-research/scripts/compare_sor_dataset.py              static-vs-adaptive comparison runner
tools/python-research/scripts/train_models.py                     offline training script
tools/python-research/requirements.txt                    Python dependencies
tools/notebook-helpers/adaptive_quantum_sor_notebooks/     notebook-only widgets and report helpers
tools/notebook-helpers/adaptive_quantum_sor_notebooks/notebook_client.py SorNotebookClient API helper
tools/notebook-helpers/adaptive_quantum_sor_notebooks/live_monitor.py live_stats_monitor dashboard templates
tools/notebook-helpers/adaptive_quantum_sor_notebooks/scenario_report.py  scenario_runner notebook report templates
tools/notebook-helpers/adaptive_quantum_sor_notebooks/submit_order_report.py submit_parent_order report templates
```

Generated `__pycache__` folders can be ignored.

## Setup

From the repository root:

```bash
python3 -m venv .venv-notebook
.venv-notebook/bin/python -m pip install -r tools/python-research/requirements.txt
export PYTHONPATH="$PWD/tools/notebook-helpers:$PWD/tools/python-research${PYTHONPATH:+:$PYTHONPATH}"
```

When using `scripts/start-jupyter-lab.sh`, `PYTHONPATH=tools/notebook-helpers:tools/python-research` is set
automatically and dependencies are installed into `.venv-notebook`.

## Helper Package

Common research imports:

```python
from adaptive_quantum_sor_research import (
    load_scenarios,
    parent_order_suggestions,
    read_feature_dataframe,
    search_scenarios,
    validate_feature_dataframe,
    write_model_predictions_artifact,
)
```

`SorNotebookClient` is used by notebooks and scripts to call Java HTTP APIs.
Order/status/control methods work with the built-in user HTTP module
(`sor-transport-http-control`) when the server exposes those endpoints:

```python
from adaptive_quantum_sor_notebooks import SorNotebookClient

client = SorNotebookClient("http://127.0.0.1:8080")
client.health()
client.ready()
client.submit_order({"instrumentId": 0, "side": "BUY", "quantity": 1000, "urgencyId": 1})
client.order_status(1)
client.policy_dataframe()
client.control_state()
client.market_data_snapshot()
client.reset_engine(mode="APPEND", reason="readme smoke")
client.metrics_text()
```

Some helpers target the test-server/demo API rather than the built-in user HTTP
module:

```python
client.stats_dataframe()       # test-server/demo /stats/current
client.order_summary_frames()  # test-server/demo /orders/summary
client.policy_dataframe()
```

Scenario helpers are test-server/demo-only because `/scenario/*` belongs to
`sor-test-server`, not the built-in user HTTP control module:

```python
client.reset_scenario("baseline-normal-open", seed=42, ticks=10, reset_mode="PURGE_AND_REPOPULATE")
client.run_scenario(
    "baseline-normal-open",
    seed=42,
    ticks=10,
    reset_mode="PURGE_AND_REPOPULATE",
    parent_orders=[{
        "instrumentId": 0,
        "side": "BUY",
        "quantity": 1000,
        "urgencyId": 0,
        "atTick": 1,
        "submitMode": "SIMULATED",
        "clientOrderRef": "readme-parent-1",
    }],
    simulator_generated_orders=False,
)
client.scenario_summary_dataframe()
client.scenario_events_dataframe()
```

For the full Jupyter testing guide, see `notebooks/README.md`.

`SubmitParentOrderApp` owns the widget panel, submit button, failure handling,
and report tables used by `notebooks/submit_parent_order.ipynb`:

```python
from adaptive_quantum_sor_notebooks import SubmitParentOrderApp

app = SubmitParentOrderApp()
app.display_controls()
app.submit_and_display()
```

`LiveStatsDashboard` owns the controls, auto-refresh loop, order-flow charts,
fill/residual charts, and venue/instrument tables used by
`notebooks/live_stats_monitor.ipynb`:

```python
from adaptive_quantum_sor_notebooks import LiveStatsDashboard

dashboard = LiveStatsDashboard()
dashboard.display()
```

`ScenarioReportApp` owns the widget panel, HTML report templates, and
DataFrame shaping used by `notebooks/scenario_runner.ipynb`. The notebook keeps
only the workflow cells:

```python
from adaptive_quantum_sor_notebooks import ScenarioReportApp

app = ScenarioReportApp()
app.display_controls()
app.display_planned_run()
report = app.run_and_display()
```

## Scenario Catalog Library

Use the scenario catalog from Python or notebooks:

```python
from adaptive_quantum_sor_research import load_scenarios, search_scenarios, parent_order_suggestions

scenarios = load_scenarios()
liquidity = search_scenarios(scenarios, tags=["liquidity"])
parent_order_suggestions(liquidity[0])
```

The simple module command is also available:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog list
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --tag liquidity
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog suggest zero-liquidity-safe-route
```

## Datasets

The example datasets are under `tools/python-research/examples/`:

```text
tools/python-research/examples/sor_notebook_features.csv       small feature dataset
tools/python-research/examples/sor_notebook_features_large.csv scenario-rich research dataset
tools/python-research/examples/sor_notebook_features_100k.csv  larger comparison/training dataset
```

Load a dataset:

```python
from adaptive_quantum_sor_research import read_feature_dataframe

features = read_feature_dataframe("tools/python-research/examples/sor_notebook_features_large.csv")
features.head()
```

Validate records:

```python
from adaptive_quantum_sor_research import validate_feature_dataframe

validate_feature_dataframe(features)
```

## Generate Datasets

Create or refresh a deterministic dataset:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 tools/python-research/scripts/generate_sor_dataset.py \
  --rows 100000 \
  --output tools/python-research/examples/sor_notebook_features_100k.csv
```

Create a larger local research dataset:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 tools/python-research/scripts/generate_sor_dataset.py \
  --rows 1000000 \
  --output build/ml/sor_notebook_features_1m.csv
```

## Train Models

Run the offline training job:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 tools/python-research/scripts/train_models.py \
  --dataset tools/python-research/examples/sor_notebook_features_100k.csv \
  --output-dir build/ml/artifacts
```

Expected outputs:

```text
build/ml/artifacts/fill_probability.json
build/ml/artifacts/toxicity.json
build/ml/artifacts/slippage.json
build/ml/artifacts/regime.json
build/ml/artifacts/predictions.csv
build/ml/artifacts/validation_metrics.csv
build/ml/artifacts/model_metadata.properties
```

The metadata file records the feature schema version and SHA-256 checksum for
`predictions.csv`. Java import validation uses both fields.

## Compare Static SOR vs Adaptive SOR

Run the comparison profile:

```bash
scripts/run_benchmarks.sh comparison
```

Default report:

```text
build/reports/benchmarks/sor-comparison-report.md
```

Override dataset or report path:

```bash
ADAPTIVE_QUANTUM_SOR_COMPARISON_DATASET=tools/python-research/examples/sor_notebook_features_100k.csv \
ADAPTIVE_QUANTUM_SOR_COMPARISON_REPORT=build/reports/benchmarks/custom-report.md \
scripts/run_benchmarks.sh comparison
```

## Create A Java-Importable Model Artifact

From a script or notebook:

```python
import pandas as pd
from adaptive_quantum_sor_research import write_model_predictions_artifact

predictions = pd.DataFrame([
    {"instrument_id": 0, "venue_id": 0, "regime_id": 0, "venue_score_bps": 7000},
    {"instrument_id": 0, "venue_id": 1, "regime_id": 0, "venue_score_bps": 6200},
    {"instrument_id": 0, "venue_id": 4, "regime_id": 3, "venue_score_bps": 1500},
])

write_model_predictions_artifact(
    predictions.to_dict("records"),
    "build/ml/notebook-artifact",
)
```

This writes:

```text
build/ml/notebook-artifact/predictions.csv
build/ml/notebook-artifact/model_metadata.properties
```

The Java importer rejects artifacts with checksum failures, schema mismatches,
NaN scores, or scores outside `[0,10000]`.

## Validate Before Handoff

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 - <<'PY'
from adaptive_quantum_sor_research import validate_feature_records
import csv

with open("tools/python-research/examples/sor_notebook_features_large.csv", newline="", encoding="utf-8") as handle:
    validate_feature_records(list(csv.DictReader(handle)))
print("feature dataset valid")
PY

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test --tests com.nitroj.adaptive.quantum.sor.ml.PythonTrainingPipelineTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test --tests com.nitroj.adaptive.quantum.sor.ml.ModelArtifactImporterTest
```
