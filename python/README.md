# Python Research Helpers

This directory contains the Python helper package, datasets, and research
scripts for Adaptive Quantum SOR. It supports notebooks, offline feature
analysis, static-vs-adaptive comparison, and model-artifact generation.

The Jupyter workflow guide now lives in `notebooks/README.md`. The notebooks
use pandas plus ipywidgets for friendly control panels and report-style output.

## What Is In This Folder

```text
python/adaptive_quantum_sor/               local notebook and pandas helper package
python/adaptive_quantum_sor/client.py      SorNotebookClient API helper
python/adaptive_quantum_sor/dataframe.py   dataset validation and artifact helpers
python/adaptive_quantum_sor/scenario_catalog.py scenario catalog library
python/adaptive_quantum_sor/schema.py      shared feature and prediction schemas
python/examples/*.csv                      example research datasets
python/models/*.py                         simple research model components
python/generate_sor_dataset.py             deterministic dataset generator
python/compare_sor_dataset.py              static-vs-adaptive comparison runner
python/train_models.py                     offline training script
python/requirements.txt                    Python dependencies
```

Generated `__pycache__` folders can be ignored.

## Setup

From the repository root:

```bash
python3 -m venv .venv-notebook
.venv-notebook/bin/python -m pip install -r python/requirements.txt
export PYTHONPATH="$PWD/python${PYTHONPATH:+:$PYTHONPATH}"
```

When using `scripts/start-jupyter-lab.sh`, `PYTHONPATH=python` is set
automatically and dependencies are installed into `.venv-notebook`.

## Helper Package

Common imports:

```python
from adaptive_quantum_sor import (
    load_scenarios,
    parent_order_suggestions,
    SorNotebookClient,
    read_feature_dataframe,
    search_scenarios,
    validate_feature_dataframe,
    write_model_predictions_artifact,
)
```

`SorNotebookClient` is used by notebooks and scripts to call the local Java
engine API:

```python
from adaptive_quantum_sor import SorNotebookClient

client = SorNotebookClient("http://127.0.0.1:8080")
client.stats_dataframe()
client.policy_dataframe()
```

Scenario helpers are also exposed through the client:

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

## Scenario Catalog Library

Use the scenario catalog from Python or notebooks:

```python
from adaptive_quantum_sor import load_scenarios, search_scenarios, parent_order_suggestions

scenarios = load_scenarios()
liquidity = search_scenarios(scenarios, tags=["liquidity"])
parent_order_suggestions(liquidity[0])
```

The simple module command is also available:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog list
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog search --tag liquidity
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog suggest zero-liquidity-safe-route
```

## Datasets

The example datasets are under `python/examples/`:

```text
python/examples/sor_notebook_features.csv       small feature dataset
python/examples/sor_notebook_features_large.csv scenario-rich research dataset
python/examples/sor_notebook_features_100k.csv  larger comparison/training dataset
```

Load a dataset:

```python
from adaptive_quantum_sor import read_feature_dataframe

features = read_feature_dataframe("python/examples/sor_notebook_features_large.csv")
features.head()
```

Validate records:

```python
from adaptive_quantum_sor import validate_feature_dataframe

validate_feature_dataframe(features)
```

## Generate Datasets

Create or refresh a deterministic dataset:

```bash
PYTHONPATH=python python3 python/generate_sor_dataset.py \
  --rows 100000 \
  --output python/examples/sor_notebook_features_100k.csv
```

Create a larger local research dataset:

```bash
PYTHONPATH=python python3 python/generate_sor_dataset.py \
  --rows 1000000 \
  --output build/ml/sor_notebook_features_1m.csv
```

## Train Models

Run the offline training job:

```bash
PYTHONPATH=python python3 python/train_models.py \
  --dataset python/examples/sor_notebook_features_100k.csv \
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
ADAPTIVE_QUANTUM_SOR_COMPARISON_DATASET=python/examples/sor_notebook_features_100k.csv \
ADAPTIVE_QUANTUM_SOR_COMPARISON_REPORT=build/reports/benchmarks/custom-report.md \
scripts/run_benchmarks.sh comparison
```

## Create A Java-Importable Model Artifact

From a script or notebook:

```python
import pandas as pd
from adaptive_quantum_sor import write_model_predictions_artifact

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
PYTHONPATH=python python3 - <<'PY'
from adaptive_quantum_sor import validate_feature_records
import csv

with open("python/examples/sor_notebook_features_large.csv", newline="", encoding="utf-8") as handle:
    validate_feature_records(list(csv.DictReader(handle)))
print("feature dataset valid")
PY

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test --tests com.nitroj.adaptive.quantum.sor.ml.PythonTrainingPipelineTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test --tests com.nitroj.adaptive.quantum.sor.ml.ModelArtifactImporterTest
```
