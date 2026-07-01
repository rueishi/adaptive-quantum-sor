# Jupyter Notebook User Guide

This folder contains the notebook workflows for live SOR demos, scenario
experiments, and research inspection. The notebooks call Java HTTP APIs and use
the helper package in `tools/notebook-helpers/adaptive_quantum_sor_notebooks`.

The notebooks are for testing, research, and observability. They are not part of
the production hot path.

`SorNotebookClient` can call supported built-in user HTTP endpoints such as
`/orders`, `/policy/current`, `/control/state`, `/control/market-data`,
`/control/reset`, `/healthz`, `/ready`, and `/metrics`. Scenario helpers such
as `/scenario/run`, `/scenario/reset`, `/scenario/summary`, and
`/scenario/events` are test-server/demo-only and are provided by
`sor-test-server`.

## What Is In This Folder

```text
notebooks/submit_parent_order.ipynb             widget-driven parent order submission report
notebooks/live_stats_monitor.ipynb              widget-driven stats, policy, and order summary monitor
notebooks/scenario_runner.ipynb                 widget-driven scenario execution report
```

Related files outside this folder:

```text
scripts/start-jupyter-lab.sh                    starts the notebook API and JupyterLab
tools/notebook-helpers/adaptive_quantum_sor_notebooks/notebook_client.py  notebook API client
tools/notebook-helpers/adaptive_quantum_sor_notebooks/live_monitor.py     live dashboard widgets and templates
tools/notebook-helpers/adaptive_quantum_sor_notebooks/scenario_report.py  scenario runner widgets and report templates
tools/python-research/examples/*.csv                           research datasets
sor-test-server/src/main/resources/scenarios/**/*.yaml
                                                readable scenario definitions
tools/python-research/adaptive_quantum_sor_research/scenario_catalog.py   scenario catalog library
```

## Prerequisites

From the repository root:

```bash
java -version
python3 --version
```

The launcher creates `.venv-notebook`, installs `tools/python-research/requirements.txt`
including pandas and ipywidgets, installs JupyterLab there, and sets
`PYTHONPATH=tools/notebook-helpers:tools/python-research` automatically. If you run
notebooks or Python manually, set it yourself:

```bash
export PYTHONPATH="$PWD/tools/notebook-helpers:$PWD/tools/python-research${PYTHONPATH:+:$PYTHONPATH}"
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
4. Starts `NotebookScenarioApiLauncher` on `http://127.0.0.1:8080`.
5. Sets `PYTHONPATH=tools/notebook-helpers:tools/python-research`.
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
The notebook delegates controls, submission, failure handling, and report
rendering to `tools/notebook-helpers/adaptive_quantum_sor_notebooks/submit_order_report.py`.

Typical flow:

1. Start JupyterLab with `scripts/start-jupyter-lab.sh`.
2. Open `submit_parent_order.ipynb`.
3. Run the setup cell.
4. Choose order values in the widget control panel.
5. Submit the order and review the generated report.
6. Inspect current stats and policy in `live_stats_monitor.ipynb`.

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

## Live Dashboard Monitoring

Use `live_stats_monitor.ipynb` when you want a periodically refreshing view of
order flow, fill progress, venue activity, instrument activity, and top
instrument-venue routes.

Typical flow:

1. Start JupyterLab with `scripts/start-jupyter-lab.sh`.
2. Open `live_stats_monitor.ipynb`.
3. Run the dashboard setup cell.
4. Choose API URL, timeout, refresh interval, and top-row limit.
5. Click `Start dashboard`.
6. Click `Stop` to pause refresh or `Reset` to stop and clear the output.

The notebook delegates controls, auto-refresh, chart rendering, and tables to
`tools/notebook-helpers/adaptive_quantum_sor_notebooks/live_monitor.py`.

## Scenario Testing

Use `scenario_runner.ipynb` when you want a controlled scenario environment.
Scenario setup changes market state, venue behavior, feed behavior, and related
simulation inputs. Parent order intents can be included directly in
`run_scenario(..., parent_orders=[...])` so the result shows route evidence for
the order you edited in the notebook. Live scenario runs require either at least
one explicit parent order or `simulator_generated_orders=True`.

The notebook delegates its widget setup, HTML templates, DataFrame shaping, and
report rendering to `tools/notebook-helpers/adaptive_quantum_sor_notebooks/scenario_report.py`. That keeps
the notebook itself short and focused on choose, preview, run, and inspect.

Typical flow:

1. Choose a scenario from the widget dropdown.
2. Choose reset mode, seed, ticks, route limits, and parent-order fields.
3. Run scenario reset.
4. Run the scenario with the parent order intent you want to test.
5. Inspect the professional report: run manifest, result list, parent order
   results, venue fill breakdown, scenario summary, reset evidence, run
   evidence, and lifecycle events.

Parent order fields:

```text
instrumentId       dense instrument id
side               BUY or SELL; numeric 1/2 is also accepted
quantity           parent order quantity
urgencyId          dense urgency id
atTick             scenario tick for scheduling the parent order
submitMode         SIMULATED or API
clientOrderRef     optional notebook/user reference
```

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
from adaptive_quantum_sor_research import load_scenarios, search_scenarios, parent_order_suggestions

scenarios = load_scenarios()
liquidity = search_scenarios(scenarios, tags=["liquidity"])
parent_order_suggestions(liquidity[0])
```

List all scenarios:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog list
```

Search by tag:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --tag liquidity
```

Search by category:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --category live-reset
```

Search descriptions:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --text auction
```

Show suggested parent order submissions for a scenario:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog suggest zero-liquidity-safe-route
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
from adaptive_quantum_sor_research import read_feature_dataframe

features = read_feature_dataframe("tools/python-research/examples/sor_notebook_features_large.csv")
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

Purpose: submit parent orders into the SOR control-plane API through a widget
control panel and inspect a polished submission report.

Expected result: a report with API manifest, exact payload, result list,
submit response, order status, child-order evidence, and KPI cards for filled
and remaining quantity.

### `live_stats_monitor.ipynb`

Purpose: inspect current SOR policy, stats, optional cumulative order summary,
and routing activity through a widget-driven monitor.

Expected result: a live dashboard with KPI cards, order-flow and fill-progress
charts, venue/instrument charts, top instrument-venue route table, and compact
stats/policy evidence tables. The generic numeric metric chart is intentionally
not used.

### `scenario_runner.ipynb`

Purpose: reset live demo scenario state, run a fixed-seed scenario selected
from widgets, and produce a user-friendly scenario execution report.

Expected result: reset summary, scenario run result, replay-safety status, and
scenario events, including run manifest, result list, parent order route
evidence, and venue-level fill rows with instrument/venue names, filled
quantity, price, and notional.
`APPEND` mode is intentionally not replay-safe because it preserves existing
live state.

Implementation note: report implementation lives in
`tools/notebook-helpers/adaptive_quantum_sor_notebooks/scenario_report.py`; the notebook should remain a
thin workflow over `ScenarioReportApp`.

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
