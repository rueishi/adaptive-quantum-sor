# Jupyter Notebook User Guide

This folder contains the notebook workflows for live SOR demos, scenario
experiments, and research inspection. The notebooks call the local Java engine
API and use the helper package in `python/adaptive_quantum_sor`.

The notebooks are for testing, research, and observability. They are not part of
the production hot path.

## What Is In This Folder

```text
notebooks/submit_parent_order.ipynb             parent order submission panel
notebooks/live_stats_monitor.ipynb              stats and policy monitor
notebooks/scenario_runner.ipynb                 scenario reset, run, and result review
```

Related files outside this folder:

```text
scripts/start-jupyter-lab.sh                    starts the engine API and JupyterLab
python/adaptive_quantum_sor/client.py           notebook API client
python/examples/*.csv                           research datasets
scenarios/**/*.yaml                             readable scenario definitions
python/adaptive_quantum_sor/scenario_catalog.py scenario catalog library
```

## Prerequisites

From the repository root:

```bash
java -version
python3 --version
```

The launcher creates `.venv-notebook`, installs `python/requirements.txt` and
JupyterLab there, and sets `PYTHONPATH=python` automatically. If you run
notebooks or Python manually, set it yourself:

```bash
export PYTHONPATH="$PWD/python${PYTHONPATH:+:$PYTHONPATH}"
```

## Start JupyterLab

Use the launcher from the repository root:

```bash
scripts/start-jupyter-lab.sh
```

The launcher:

1. Creates `.venv-notebook` when needed.
2. Installs notebook dependencies into that virtual environment.
3. Builds the Java classes.
4. Starts the SOR engine API on `http://127.0.0.1:8080`.
5. Sets `PYTHONPATH=python`.
6. Opens JupyterLab with the notebook workspace.

Optional ports:

```bash
ADAPTIVE_QUANTUM_SOR_API_PORT=8081 ADAPTIVE_QUANTUM_SOR_JUPYTER_PORT=8890 scripts/start-jupyter-lab.sh
```

## Recommended Notebook Layout

Open these notebooks as separate panels:

1. `submit_parent_order.ipynb`
2. `live_stats_monitor.ipynb`
3. `scenario_runner.ipynb`

Run the setup cell in each notebook first. JupyterLab usually remembers the
panel layout after you arrange it once.

## Live Order Testing

Use `submit_parent_order.ipynb` when you want to test the live order path.

Typical flow:

1. Start JupyterLab with `scripts/start-jupyter-lab.sh`.
2. Open `submit_parent_order.ipynb`.
3. Run the setup cell.
4. Edit the order payload.
5. Submit the order.
6. Fetch the order status.
7. Inspect current stats and policy in `live_stats_monitor.ipynb`.

Example parent order payload:

```python
order = {
    "instrumentId": 0,
    "side": 1,
    "quantity": 1000,
    "urgencyId": 0,
}
```

Expected result: the API returns a response with `parentOrderId`,
`remainingQty`, and `status`.

## Scenario Testing

Use `scenario_runner.ipynb` when you want a controlled scenario environment.
Scenario setup changes market state, venue behavior, feed behavior, and related
simulation inputs. Parent order intents can be included directly in
`run_scenario(..., parent_orders=[...])` so the result shows route evidence for
the order you edited in the notebook.

Typical flow:

1. Choose a scenario from `scenarios/**/*.yaml`.
2. Choose a reset mode.
3. Run scenario reset.
4. Run the scenario with the parent order intent you want to test.
5. Inspect scenario summary, events, order results, and policy behavior.

Reset modes:

```text
ISOLATED                 test-only isolated state boundary
PURGE_AND_REPOPULATE     clear live engine stats and rebuild baseline state
KEEP_POLICY_PURGE_STATS  keep current policy, clear stats
APPEND                   append to current live state, not replay-safe
```

For live notebook work, prefer `PURGE_AND_REPOPULATE` when you want the user to
see exactly what was cleared and rebuilt before the scenario run.

## Scenario Catalog Library And Commands

From notebook Python:

```python
from adaptive_quantum_sor import load_scenarios, search_scenarios, parent_order_suggestions

scenarios = load_scenarios()
liquidity = search_scenarios(scenarios, tags=["liquidity"])
parent_order_suggestions(liquidity[0])
```

List all scenarios:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog list
```

Search by tag:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog search --tag liquidity
```

Search by category:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog search --category live-reset
```

Search descriptions:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog search --text auction
```

Show suggested parent order submissions for a scenario:

```bash
PYTHONPATH=python python3 -m adaptive_quantum_sor.scenario_catalog suggest zero-liquidity-safe-route
```

Use these suggestions as starting points, then adjust instrument, side,
quantity, and urgency for the behavior you want to validate.

## Scenario Replay Flow

Scenario replay is for automated and deterministic validation. It should be
repeatable from a seed and scenario file.

Replay flow:

```text
load scenario file
validate metadata, category, tags, phases, and assertions
reset the state boundary
replay deterministic ticks and venue/feed behavior
apply explicit parent order intents when present
collect route, fill, reject, residual, policy, and lifecycle evidence
compare result evidence to assertions
```

This is different from exploratory live testing. Live testing can still be
deterministic when using purge/reset and fixed scenario files, but the notebook
should make every reset and parent order submission visible to the user.

## Research Dataset Inspection

In any notebook:

```python
from adaptive_quantum_sor import read_feature_dataframe

features = read_feature_dataframe("python/examples/sor_notebook_features_large.csv")
features.head()
```

Useful first cuts:

```python
features.groupby("regime_id")[["label_fill_bps", "label_slippage_bps", "label_toxicity_bps"]].mean()
features.groupby("venue_id")[["fill_probability_bps", "reject_rate_bps", "toxicity_bps"]].mean()
features.pivot_table(index="regime_id", columns="venue_id", values="label_fill_bps", aggfunc="mean")
```

What to look for:

```text
calm regime: tight spreads, higher fill labels, lower toxicity
volatile regime: wider spreads and higher slippage
toxic/adverse regime: higher toxicity labels and weaker venue outcomes
liquidity stress regime: higher reject rates and residual quantities
```

## Notebook Reference

### `submit_parent_order.ipynb`

Purpose: submit parent orders into the SOR control-plane API and inspect the
accepted order response.

Expected result: a JSON response containing `parentOrderId`, `remainingQty`,
and `status`.

### `live_stats_monitor.ipynb`

Purpose: inspect current SOR policy and stats as pandas tables.

Expected result: pandas DataFrames from `/stats/current` and
`/policy/current`, including policy version and venue count.

### `scenario_runner.ipynb`

Purpose: reset live demo scenario state, run a fixed-seed scenario, and inspect
the scenario summary and lifecycle events.

Expected result: reset summary, scenario run result, replay-safety status, and
scenario events. `APPEND` mode is intentionally not replay-safe because it
preserves existing live state.

## Troubleshooting

If JupyterLab or pandas is missing, rerun the launcher. It installs both into
`.venv-notebook`:

```bash
scripts/start-jupyter-lab.sh
```

To force a clean dependency reinstall:

```bash
rm -rf .venv-notebook
scripts/start-jupyter-lab.sh
```

If the notebooks cannot reach the API, confirm the launcher is still running:

```bash
curl http://127.0.0.1:8080/stats/current
```

If port `8080` or `8888` is already in use:

```bash
ADAPTIVE_QUANTUM_SOR_API_PORT=8081 ADAPTIVE_QUANTUM_SOR_JUPYTER_PORT=8890 scripts/start-jupyter-lab.sh
```
