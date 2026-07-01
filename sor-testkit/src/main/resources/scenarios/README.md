# Scenario Catalog

This directory contains user-readable scenario metadata files organized by
category subfolder. Each `sor-testkit/src/main/resources/scenarios/<category>/*.yaml` file is loaded by
`ScenarioDefinitionLoader`, converted to `ScenarioSpec`, and run through
`ScenarioRunner`.

The runtime loader uses:

```text
scenarioId
description
category
tags
seed
ticks
venueProfilesEnabled
windows[].startTick
windows[].endTick
windows[].regime
```

The `category` field must match the subfolder name. The `tags` and `expected`
sections are intentionally user-facing documentation. They make the scenario
purpose easy to scan and can be promoted to executable assertions later.

## Categories

```text
baseline and replay
regime transition and flapping
liquidity disappearance and residual quantity
stale feed and feed recovery
venue outage and recovery
latency, toxicity, rejects, and venue health
auction, halt, open, and close behavior
optimizer lineage, tie-breaks, and policy safety
quadratic QUBO venue interactions and concentration/anti-gaming risk
live reset modes for notebook/API testing
ML dataset and label generation
risk, throttle, capacity, and oversized parent orders
```

Every YAML file in this catalog is loaded and replayed deterministically by:

```bash
./gradlew test --tests 'com.nitroj.sor.testkit.scenario.ScenarioDefinitionLoaderTest'
```

## Catalog Library And Command

Use the Python library from notebooks or research scripts:

```python
from adaptive_quantum_sor_research import load_scenarios, search_scenarios, parent_order_suggestions

scenarios = load_scenarios()
liquidity = search_scenarios(scenarios, tags=["liquidity"])
parent_order_suggestions(liquidity[0])
```

Use the simple module command from the shell.

List every scenario with category, tags, path, and description:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog list
```

Search by tag/category/text:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --tag liquidity
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --category live-reset
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog search --text auction
```

Show scenario details plus suggested parent order submissions for notebook/API
testing:

```bash
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog suggest zero-liquidity-safe-route
PYTHONPATH=tools/notebook-helpers:tools/python-research python3 -m adaptive_quantum_sor_research.scenario_catalog suggest high-urgency-sweep
```

Scenario files may include optional `parentOrders` defaults. Notebook users can
override these at run time:

```yaml
parentOrders:
  - instrumentId: 0
    side: BUY
    quantity: 4000
    urgency: NORMAL
    atTick: 1
    submitMode: SIMULATED
    clientOrderRef: baseline-normal-buy
```
