#!/usr/bin/env python3
"""Train deterministic Phase 4 Adaptive Quantum SOR model artifacts from an exported CSV dataset."""

import argparse
import csv
import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from models import fill_probability, regime, slippage, toxicity

SCHEMA_VERSION = "feature-schema-v1"
REQUIRED_COLUMNS = {
    "schema_version",
    "instrument_id",
    "venue_id",
    "regime_id",
    "label_fill_bps",
    "label_slippage_bps",
    "label_toxicity_bps",
    "label_regime_id",
}


def read_dataset(path):
    if not path.is_file():
        raise ValueError(f"dataset missing: {path}")
    with path.open(newline="", encoding="utf-8") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames is None:
            raise ValueError("dataset header missing")
        missing = REQUIRED_COLUMNS.difference(reader.fieldnames)
        if missing:
            raise ValueError("dataset missing required columns: " + ",".join(sorted(missing)))
        rows = list(reader)
    if not rows:
        raise ValueError("dataset has no rows")
    versions = {row["schema_version"] for row in rows}
    if versions != {SCHEMA_VERSION}:
        raise ValueError("dataset schema mismatch: " + ",".join(sorted(versions)))
    return rows


def bounded(value):
    return max(0, min(10_000, int(value)))


def write_json(path, payload):
    path.write_text(json.dumps(payload, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def train(dataset, output_dir):
    rows = read_dataset(dataset)
    output_dir.mkdir(parents=True, exist_ok=True)

    models = {
        "fill_probability": fill_probability.train(rows),
        "toxicity": toxicity.train(rows),
        "slippage": slippage.train(rows),
        "regime": regime.train(rows),
    }
    for name, payload in models.items():
        write_json(output_dir / f"{name}.json", payload)

    predictions = output_dir / "predictions.csv"
    with predictions.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["instrument_id", "venue_id", "regime_id", "venue_score_bps"])
        for row in rows:
            fill_bps = bounded(fill_probability.predict(models["fill_probability"], row))
            tox_bps = bounded(toxicity.predict(models["toxicity"], row))
            slip_bps = bounded(slippage.predict(models["slippage"], row))
            score = bounded(5_000 + (fill_bps - 5_000) // 2 - tox_bps // 4 - slip_bps // 4)
            writer.writerow([row["instrument_id"], row["venue_id"], row["regime_id"], score])

    metrics = output_dir / "validation_metrics.csv"
    with metrics.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["metric", "value"])
        writer.writerow(["row_count", len(rows)])
        writer.writerow(["fill_probability_bps", models["fill_probability"]["fill_probability_bps"]])
        writer.writerow(["toxicity_bps", models["toxicity"]["toxicity_bps"]])
        writer.writerow(["slippage_bps", models["slippage"]["slippage_bps"]])
        writer.writerow(["regime_id", models["regime"]["regime_id"]])

    metadata = output_dir / "model_metadata.properties"
    prediction_checksum = sha256(predictions)
    metadata.write_text(
        "\n".join(
            [
                "modelVersion=1",
                f"featureSchemaVersion={SCHEMA_VERSION}",
                "predictionFile=predictions.csv",
                f"predictionChecksumSha256={prediction_checksum}",
                f"rowCount={len(rows)}",
                "",
            ]
        ),
        encoding="utf-8",
    )
    return 0


def main(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args(argv)
    try:
        return train(args.dataset, args.output_dir)
    except Exception as exc:
        print(f"training failed: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
