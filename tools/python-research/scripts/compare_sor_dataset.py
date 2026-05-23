#!/usr/bin/env python3
"""Run static-vs-adaptive SOR comparison over a feature CSV and write Markdown."""

import argparse
import csv
from collections import defaultdict
from pathlib import Path

from adaptive_quantum_sor_research.schema import FEATURE_SCHEMA_VERSION


GROUP_KEYS = ("parent_order_id", "instrument_id", "regime_id")


def int_value(row, key):
    return int(row[key])


def load_groups(dataset):
    groups = defaultdict(list)
    with dataset.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        required = {
            "schema_version",
            "parent_order_id",
            "instrument_id",
            "venue_id",
            "regime_id",
            "ask_price_ticks",
            "spread_ticks",
            "ask_qty",
            "latency_nanos",
            "fill_probability_bps",
            "toxicity_bps",
            "reject_rate_bps",
            "slippage_bps",
            "label_fill_bps",
            "label_slippage_bps",
            "label_toxicity_bps",
        }
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError("dataset missing required columns: " + ",".join(sorted(missing)))
        for row in reader:
            if row["schema_version"] != FEATURE_SCHEMA_VERSION:
                raise ValueError("schema mismatch: " + row["schema_version"])
            groups[tuple(row[key] for key in GROUP_KEYS)].append(row)
    if not groups:
        raise ValueError("dataset has no comparison groups")
    return groups


def static_choice(rows):
    """Static baseline: choose best displayed price/spread, then venue ID."""

    return min(
        rows,
        key=lambda row: (
            int_value(row, "ask_price_ticks"),
            int_value(row, "spread_ticks"),
            int_value(row, "venue_id"),
        ),
    )


def adaptive_choice(rows):
    """Adaptive route: choose highest expected quality from feature columns."""

    return max(
        rows,
        key=lambda row: (
            int_value(row, "fill_probability_bps")
            - int_value(row, "toxicity_bps")
            - int_value(row, "reject_rate_bps")
            - int_value(row, "slippage_bps") * 45
            - int_value(row, "latency_nanos") // 10_000
            + min(int_value(row, "ask_qty"), 250_000) // 5_000,
            -int_value(row, "venue_id"),
        ),
    )


def run(groups):
    static_totals = totals()
    adaptive_totals = totals()
    wins = {"adaptive": 0, "static": 0, "tie": 0}

    for rows in groups.values():
        static_row = static_choice(rows)
        adaptive_row = adaptive_choice(rows)
        add(static_totals, static_row)
        add(adaptive_totals, adaptive_row)
        static_score = realized_score(static_row)
        adaptive_score = realized_score(adaptive_row)
        if adaptive_score > static_score:
            wins["adaptive"] += 1
        elif static_score > adaptive_score:
            wins["static"] += 1
        else:
            wins["tie"] += 1
    return static_totals, adaptive_totals, wins


def totals():
    return {
        "orders": 0,
        "fill_bps": 0,
        "slippage_bps": 0,
        "toxicity_bps": 0,
        "reject_count": 0,
    }


def add(target, row):
    target["orders"] += 1
    target["fill_bps"] += int_value(row, "label_fill_bps")
    target["slippage_bps"] += int_value(row, "label_slippage_bps")
    target["toxicity_bps"] += int_value(row, "label_toxicity_bps")
    if int_value(row, "label_fill_bps") == 0:
        target["reject_count"] += 1


def realized_score(row):
    return (
        int_value(row, "label_fill_bps")
        - int_value(row, "label_slippage_bps") * 45
        - int_value(row, "label_toxicity_bps")
        - (2_000 if int_value(row, "label_fill_bps") == 0 else 0)
    )


def avg(total, key):
    return round(total[key] / total["orders"])


def reject_rate(total):
    return round(total["reject_count"] * 10_000 / total["orders"])


def improvement(static, adaptive):
    return (
        avg(adaptive, "fill_bps")
        - avg(static, "fill_bps")
        + avg(static, "slippage_bps")
        - avg(adaptive, "slippage_bps")
        + reject_rate(static)
        - reject_rate(adaptive)
    )


def markdown(dataset, groups, static, adaptive, wins):
    return f"""# SOR Static vs Adaptive Comparison Report

Dataset: `{dataset}`

## Summary

| Metric | Static SOR | Adaptive SOR |
| --- | ---: | ---: |
| Orders compared | {static['orders']} | {adaptive['orders']} |
| Average fill bps | {avg(static, 'fill_bps')} | {avg(adaptive, 'fill_bps')} |
| Average slippage bps | {avg(static, 'slippage_bps')} | {avg(adaptive, 'slippage_bps')} |
| Average toxicity bps | {avg(static, 'toxicity_bps')} | {avg(adaptive, 'toxicity_bps')} |
| Reject/no-fill rate bps | {reject_rate(static)} | {reject_rate(adaptive)} |

Realized improvement bps: **{improvement(static, adaptive)}**

## Winner Counts

| Winner | Count |
| --- | ---: |
| Adaptive | {wins['adaptive']} |
| Static | {wins['static']} |
| Tie | {wins['tie']} |

## Method

Both routers are evaluated against the same grouped feature rows.

Static SOR chooses the venue with the best displayed ask price, then tighter
spread, then lowest venue ID.

Adaptive SOR chooses the venue with the highest feature quality score using fill
probability, toxicity, reject rate, slippage, latency, and displayed liquidity.

Actual outcome is scored from the selected row labels:

```text
label_fill_bps
label_slippage_bps
label_toxicity_bps
zero-fill/no-fill penalty
```

Comparison groups: {len(groups)}
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", type=Path, default=Path("tools/python-research/examples/sor_notebook_features_100k.csv"))
    parser.add_argument("--output", type=Path, default=Path("build/reports/benchmarks/sor-comparison-report.md"))
    args = parser.parse_args()
    groups = load_groups(args.dataset)
    static, adaptive, wins = run(groups)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(markdown(args.dataset, groups, static, adaptive, wins), encoding="utf-8")
    print(f"wrote {args.output}")


if __name__ == "__main__":
    main()
