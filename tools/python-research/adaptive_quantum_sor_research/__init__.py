"""Research, dataset, and scenario-catalog helpers for Adaptive Quantum SOR."""

from .dataframe import (
    read_feature_dataframe,
    records_to_dataframe,
    validate_feature_dataframe,
    validate_feature_records,
    write_feature_dataframe,
    write_feature_dataset,
    write_model_predictions_artifact,
)
from .schema import FEATURE_COLUMNS, FEATURE_SCHEMA_VERSION, ORDER_COLUMNS, PREDICTION_COLUMNS

_SCENARIO_EXPORTS = {
    "Scenario",
    "find_scenario",
    "format_scenarios",
    "load_scenarios",
    "parent_order_suggestions",
    "search_scenarios",
}

__all__ = [
    "FEATURE_COLUMNS",
    "FEATURE_SCHEMA_VERSION",
    "ORDER_COLUMNS",
    "PREDICTION_COLUMNS",
    "Scenario",
    "find_scenario",
    "format_scenarios",
    "load_scenarios",
    "parent_order_suggestions",
    "read_feature_dataframe",
    "records_to_dataframe",
    "search_scenarios",
    "validate_feature_dataframe",
    "validate_feature_records",
    "write_feature_dataframe",
    "write_feature_dataset",
    "write_model_predictions_artifact",
]


def __getattr__(name: str):
    if name in _SCENARIO_EXPORTS:
        from . import scenario_catalog

        return getattr(scenario_catalog, name)
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
