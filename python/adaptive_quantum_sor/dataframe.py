"""Pandas-friendly helpers for SOR feature datasets and model artifacts."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path
from typing import Iterable, Mapping

from .schema import FEATURE_COLUMNS, FEATURE_SCHEMA_VERSION, PREDICTION_COLUMNS


def _require_pandas():
    try:
        import pandas as pd  # type: ignore
    except ModuleNotFoundError as exc:
        raise ModuleNotFoundError(
            "pandas is required for DataFrame helpers. Install with `pip install -r python/requirements.txt`."
        ) from exc
    return pd


def read_feature_dataframe(path: str | Path):
    """Load an exported SOR feature dataset as a validated pandas DataFrame."""

    pd = _require_pandas()
    frame = pd.read_csv(path)
    validate_feature_dataframe(frame)
    return frame


def write_feature_dataframe(frame, path: str | Path) -> Path:
    """Validate and write a pandas DataFrame as a SOR feature dataset CSV."""

    validate_feature_dataframe(frame)
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    frame.to_csv(target, index=False, columns=FEATURE_COLUMNS)
    return target


def validate_feature_dataframe(frame) -> None:
    """Validate required columns, schema version, and bounded label values."""

    missing = [column for column in FEATURE_COLUMNS if column not in frame.columns]
    if missing:
        raise ValueError("feature DataFrame missing columns: " + ",".join(missing))
    versions = set(str(value) for value in frame["schema_version"].dropna().unique())
    if versions != {FEATURE_SCHEMA_VERSION}:
        raise ValueError("feature DataFrame schema mismatch: " + ",".join(sorted(versions)))
    for column in ("label_fill_bps", "label_slippage_bps", "label_toxicity_bps"):
        invalid = frame[(frame[column] < 0) | (frame[column] > 10_000)]
        if len(invalid) != 0:
            raise ValueError(f"{column} must be in [0,10000]")


def records_to_dataframe(records: Iterable[Mapping[str, object]]):
    """Create a pandas DataFrame from notebook dictionaries and validate it."""

    pd = _require_pandas()
    frame = pd.DataFrame(list(records), columns=FEATURE_COLUMNS)
    validate_feature_dataframe(frame)
    return frame


def validate_feature_records(records: Iterable[Mapping[str, object]]) -> list[dict[str, object]]:
    """Validate feature rows without requiring pandas."""

    normalized = [dict(record) for record in records]
    if not normalized:
        raise ValueError("feature records must not be empty")
    for index, record in enumerate(normalized, start=1):
        missing = [column for column in FEATURE_COLUMNS if column not in record]
        if missing:
            raise ValueError(f"feature record {index} missing columns: " + ",".join(missing))
        if record["schema_version"] != FEATURE_SCHEMA_VERSION:
            raise ValueError(f"feature record {index} schema mismatch: {record['schema_version']}")
        for column in ("label_fill_bps", "label_slippage_bps", "label_toxicity_bps"):
            value = int(record[column])
            if value < 0 or value > 10_000:
                raise ValueError(f"{column} must be in [0,10000]")
    return normalized


def write_feature_dataset(records: Iterable[Mapping[str, object]], path: str | Path) -> Path:
    """Write feature records as the same CSV format exported by Java."""

    rows = validate_feature_records(records)
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FEATURE_COLUMNS)
        writer.writeheader()
        writer.writerows(rows)
    return target


def write_model_predictions_artifact(
    predictions: Iterable[Mapping[str, object]],
    output_dir: str | Path,
    model_version: int = 1,
    feature_schema_version: str = FEATURE_SCHEMA_VERSION,
) -> Path:
    """Write predictions.csv plus metadata for Java ModelArtifactImporter."""

    rows = [dict(row) for row in predictions]
    if not rows:
        raise ValueError("prediction rows must not be empty")
    for index, row in enumerate(rows, start=1):
        missing = [column for column in PREDICTION_COLUMNS if column not in row]
        if missing:
            raise ValueError(f"prediction row {index} missing columns: " + ",".join(missing))
        score = row["venue_score_bps"]
        if str(score).lower() in {"nan", "infinity", "-infinity"}:
            raise ValueError("venue_score_bps must be finite")
        score_int = int(score)
        if score_int < 0 or score_int > 10_000:
            raise ValueError("venue_score_bps must be in [0,10000]")
        row["venue_score_bps"] = score_int

    directory = Path(output_dir)
    directory.mkdir(parents=True, exist_ok=True)
    predictions_file = directory / "predictions.csv"
    with predictions_file.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=PREDICTION_COLUMNS)
        writer.writeheader()
        writer.writerows(rows)

    checksum = hashlib.sha256(predictions_file.read_bytes()).hexdigest()
    metadata = directory / "model_metadata.properties"
    metadata.write_text(
        "\n".join(
            [
                f"modelVersion={model_version}",
                f"featureSchemaVersion={feature_schema_version}",
                "predictionFile=predictions.csv",
                f"predictionChecksumSha256={checksum}",
                f"rowCount={len(rows)}",
                "",
            ]
        ),
        encoding="utf-8",
    )
    return directory
